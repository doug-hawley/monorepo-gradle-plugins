package io.github.doughawley.monorepobuild

import io.github.doughawley.monorepobuild.git.GitCommandExecutor
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger

/**
 * Gradle plugin that detects which projects have changed based on git history.
 */
class MonorepoBuildPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // Register the extension on the root project to ensure it's shared
        val rootExtension = if (project == project.rootProject) {
            project.extensions.create(
                "monorepoBuild",
                MonorepoBuildExtension::class.java
            )
        } else {
            // If applied to subproject, get or create extension on root
            project.rootProject.extensions.findByType(MonorepoBuildExtension::class.java)
                ?: project.rootProject.extensions.create(
                    "monorepoBuild",
                    MonorepoBuildExtension::class.java
                )
        }

        // Register per-project exclude extension on every subproject.
        // Subproject Project objects already exist at this point (created from settings.gradle.kts),
        // so iteration is safe without waiting for subproject evaluation.
        if (project == project.rootProject) {
            project.subprojects.forEach { subproject ->
                subproject.extensions.create("projectExcludes", ProjectExcludesExtension::class.java)
            }
        }

        // Compute metadata in configuration phase after ALL projects are evaluated.
        // Use gradle.projectsEvaluated to ensure all subprojects are configured.
        // Under --parallel, multiple threads may fire this callback concurrently.
        // computationGuard.compareAndSet(false, true) is an atomic check-and-set:
        // only the first thread to win proceeds; all others see true and skip.
        project.gradle.projectsEvaluated {
            if (rootExtension.computationGuard.compareAndSet(false, true)) {
                try {
                    computeMetadata(project.rootProject, rootExtension)
                    rootExtension.metadataComputed = true
                    project.logger.debug("Changed project metadata computed successfully in configuration phase")

                    // Wire up dependsOn for each affected project's build task now that we know
                    // which projects changed. This must happen in the configuration phase so
                    // Gradle can include them in the task graph before execution begins.
                    val buildChangedTask = project.tasks.named("buildChangedProjectsFromBranch")
                    rootExtension.allAffectedProjects.forEach { projectPath ->
                        val targetProject = project.rootProject.findProject(projectPath)
                        if (targetProject != null) {
                            val buildTask = targetProject.tasks.findByName("build")
                            if (buildTask != null) {
                                buildChangedTask.configure {
                                    dependsOn(buildTask)
                                }
                            } else {
                                project.logger.warn("No build task found for $projectPath")
                            }
                        } else {
                            project.logger.warn("Project not found: $projectPath")
                        }
                    }
                } catch (e: Exception) {
                    // Fail-fast: metadata computation is critical
                    throw IllegalStateException(
                        "Failed to compute changed project metadata in configuration phase: ${e.message}",
                        e
                    )
                }
            }
        }

        // Register the printChangedProjectsFromBranch task
        project.tasks.register("printChangedProjectsFromBranch", PrintChangedProjectsTask::class.java).configure {
            group = "verification"
            description = "Detects which projects have changed based on git history"
        }

        // Register the buildChangedProjectsFromBranch task.
        // Actual dependsOn wiring for affected project build tasks is added dynamically
        // in the projectsEvaluated hook above, after changed projects are known.
        project.tasks.register("buildChangedProjectsFromBranch").configure {
            group = "build"
            description = "Builds only the projects that have been affected by changes"
            doLast {
                val extension = project.rootProject.extensions.getByType(MonorepoBuildExtension::class.java)

                if (!extension.metadataComputed) {
                    throw IllegalStateException(
                        "Changed project metadata was not computed in the configuration phase. " +
                        "Possible causes: the plugin was not applied to the root project, " +
                        "or an error occurred during project evaluation. " +
                        "Re-run with --info or --debug for more details."
                    )
                }

                val changedProjects = extension.allAffectedProjects
                if (changedProjects.isEmpty()) {
                    project.logger.lifecycle("No projects have changed - nothing to build")
                } else {
                    project.logger.lifecycle("Building changed projects: ${changedProjects.joinToString(", ")}")
                }
            }
        }

        project.logger.info("Monorepo Build Plugin applied to ${project.name}")
    }

    /**
     * Computes changed project metadata.
     * Called during task execution to ensure all dependencies are fully resolved.
     *
     * @param project The Gradle project
     * @param extension The plugin extension
     */
    internal fun computeMetadata(project: Project, extension: MonorepoBuildExtension) {
        val logger = project.logger

        logger.info("Computing changed project metadata...")
        logger.info("Base branch: ${extension.baseBranch}")
        logger.info("Include untracked: ${extension.includeUntracked}")

        // Initialize detectors and factories, sharing a single GitCommandExecutor instance
        val gitExecutor = GitCommandExecutor(logger)
        val gitDetector = GitChangedFilesDetector(logger, gitExecutor)
        val projectMapper = ProjectFileMapper()
        val metadataFactory = ProjectMetadataFactory(logger)

        // Detect changed files from git
        val changedFiles = gitDetector.getChangedFiles(project.rootDir, extension)
        val changedFilesMap = projectMapper.mapChangedFilesToProjects(project.rootProject, changedFiles)

        // Apply per-project exclude patterns (configured via projectExcludes { } in each subproject)
        val filteredChangedFilesMap = applyPerProjectExcludes(project.rootProject, changedFilesMap, logger)

        // Build metadata with changed files information
        val metadataMap = metadataFactory.buildProjectMetadataMap(project.rootProject, filteredChangedFilesMap)

        // Get all affected projects (those with changes OR dependency changes).
        // ":" is Gradle's path for the root project — it has no dedicated build task
        // and is intentionally excluded from the affected project list.
        // Projects without a build file (build.gradle or build.gradle.kts) are also
        // excluded as they cannot be built directly.
        val allAffectedProjects = metadataMap.values
            .filter { metadata ->
                metadata.hasChanges() &&
                metadata.fullyQualifiedName != ":" &&
                hasBuildFile(project.rootProject, metadata.fullyQualifiedName).also { hasBuild ->
                    if (!hasBuild) {
                        logger.debug("Excluding ${metadata.fullyQualifiedName} from affected projects: no build file found")
                    }
                }
            }
            .map { it.fullyQualifiedName }
            .toSet()

        // Store in extension for access during configuration and execution
        extension.metadataMap = metadataMap
        extension.allAffectedProjects = allAffectedProjects
        extension.changedFilesMap = filteredChangedFilesMap

        logger.info("Changed files count: ${changedFiles.size}")
        logger.info("All affected projects (including dependents): ${allAffectedProjects.joinToString(", ").ifEmpty { "none" }}")
    }

    /**
     * Applies per-project exclude patterns to filter files from the changed files map.
     * Patterns are matched against paths relative to each project's directory,
     * so a pattern "generated/.*" in :api matches "api/generated/Code.kt".
     */
    private fun applyPerProjectExcludes(
        rootProject: Project,
        changedFilesMap: Map<String, List<String>>,
        logger: Logger
    ): Map<String, List<String>> {
        return changedFilesMap.mapValues { (projectPath, files) ->
            val targetProject = rootProject.findProject(projectPath)
            val ext = targetProject?.extensions?.findByType(ProjectExcludesExtension::class.java)
            val patterns = ext?.excludePatterns?.map { Regex(it) } ?: emptyList()
            if (patterns.isEmpty()) {
                files
            } else {
                val projectRelPath = targetProject
                    ?.projectDir?.relativeTo(rootProject.rootDir)?.path?.replace('\\', '/')
                    ?: ""
                files.filterNot { file ->
                    val localFile = if (projectRelPath.isNotEmpty() && file.startsWith("$projectRelPath/")) {
                        file.removePrefix("$projectRelPath/")
                    } else {
                        file
                    }
                    patterns.any { pattern -> localFile.matches(pattern) }
                }.also { filtered ->
                    val excluded = files.size - filtered.size
                    if (excluded > 0) {
                        logger.debug("[$projectPath] Per-project excludes removed $excluded file(s)")
                    }
                }
            }
        }
    }

    /**
     * Checks if a project has a build file (build.gradle or build.gradle.kts).
     */
    private fun hasBuildFile(rootProject: Project, projectPath: String): Boolean {
        val targetProject = rootProject.findProject(projectPath) ?: return false
        val projectDir = targetProject.projectDir
        return projectDir.resolve("build.gradle.kts").exists() ||
               projectDir.resolve("build.gradle").exists()
    }
}
