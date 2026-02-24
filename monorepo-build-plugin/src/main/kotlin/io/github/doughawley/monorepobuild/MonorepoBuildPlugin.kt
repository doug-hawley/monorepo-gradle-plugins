package io.github.doughawley.monorepobuild

import io.github.doughawley.monorepobuild.domain.ProjectFileMapper
import io.github.doughawley.monorepobuild.domain.ProjectMetadataFactory
import io.github.doughawley.monorepobuild.git.GitChangedFilesDetector
import io.github.doughawley.monorepobuild.git.GitRepository
import io.github.doughawley.monorepobuild.task.PrintChangedProjectsFromRefTask
import io.github.doughawley.monorepobuild.task.PrintChangedProjectsTask
import io.github.doughawley.monorepobuild.task.WriteChangedProjectsFromRefTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.configuration.BuildFeatures
import org.gradle.api.logging.Logger
import javax.inject.Inject

/**
 * Gradle plugin that detects which projects have changed based on git history.
 */
class MonorepoBuildPlugin @Inject constructor(
    private val buildFeatures: BuildFeatures
) : Plugin<Project> {

    private enum class DetectionMode { FROM_BRANCH, FROM_REF }

    private companion object {
        val REF_TASKS = setOf("printChangedProjectsFromRef", "buildChangedProjectsFromRef", "writeChangedProjectsFromRef")
        val BRANCH_TASKS = setOf("printChangedProjectsFromBranch", "buildChangedProjectsFromBranch")
        const val TASK_GROUP = "monorepo"
    }

    override fun apply(project: Project) {
        if (buildFeatures.configurationCache.requested.getOrElse(false)) {
            throw GradleException(
                "monorepo-build-plugin is incompatible with the Gradle configuration cache " +
                "because it executes git commands during the configuration phase. " +
                "Set org.gradle.configuration-cache=false in your gradle.properties."
            )
        }

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
                    val mode = resolveMode(project.rootProject)
                    if (mode == DetectionMode.FROM_REF) {
                        val commitRef = resolveCommitRef(project.rootProject, rootExtension)
                            ?: throw GradleException(
                                "printChangedProjectsFromRef / buildChangedProjectsFromRef / writeChangedProjectsFromRef requires " +
                                "a commitRef. Set it in the monorepoBuild DSL or pass " +
                                "-PmonorepoBuild.commitRef=<sha>."
                            )
                        rootExtension.commitRef = commitRef
                        computeMetadata(project.rootProject, rootExtension, commitRef)
                        wireDependsOn(project, "buildChangedProjectsFromRef", rootExtension.allAffectedProjects)
                    } else {
                        computeMetadata(project.rootProject, rootExtension, commitRef = null)
                        wireDependsOn(project, "buildChangedProjectsFromBranch", rootExtension.allAffectedProjects)
                    }
                    rootExtension.metadataComputed = true
                    project.logger.debug("Changed project metadata computed successfully in configuration phase")
                } catch (e: GradleException) {
                    throw e
                } catch (e: Exception) {
                    // Fail-fast: metadata computation is critical; wrap as GradleException so
                    // Gradle surfaces it cleanly rather than swallowing it during configuration.
                    throw GradleException(
                        "Failed to compute changed project metadata in configuration phase: ${e.message}",
                        e
                    )
                }
            }
        }

        // Register the printChangedProjectsFromBranch task
        project.tasks.register("printChangedProjectsFromBranch", PrintChangedProjectsTask::class.java).configure {
            group = TASK_GROUP
            description = "Detects which projects have changed based on git history"
        }

        // Register the buildChangedProjectsFromBranch task.
        // Actual dependsOn wiring for affected project build tasks is added dynamically
        // in the projectsEvaluated hook above, after changed projects are known.
        project.tasks.register("buildChangedProjectsFromBranch").configure {
            group = TASK_GROUP
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

        // Register the printChangedProjectsFromRef task
        project.tasks.register("printChangedProjectsFromRef", PrintChangedProjectsFromRefTask::class.java).configure {
            group = TASK_GROUP
            description = "Detects which projects changed since a specific commit ref"
        }

        // Register the buildChangedProjectsFromRef task.
        // Actual dependsOn wiring for affected project build tasks is added dynamically
        // in the projectsEvaluated hook above, after changed projects are known.
        project.tasks.register("buildChangedProjectsFromRef").configure {
            group = TASK_GROUP
            description = "Builds only the projects affected by changes since a specific commit ref"
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
                val ref = extension.commitRef
                if (changedProjects.isEmpty()) {
                    project.logger.lifecycle("No projects have changed - nothing to build")
                } else {
                    project.logger.lifecycle("Building changed projects (since $ref): ${changedProjects.joinToString(", ")}")
                }
            }
        }

        // Register the writeChangedProjectsFromRef task.
        // Output file defaults to build/monorepo/changed-projects.txt but can be overridden
        // via -PmonorepoBuild.outputFile=<path> at runtime or by configuring the task directly.
        project.tasks.register("writeChangedProjectsFromRef", WriteChangedProjectsFromRefTask::class.java).configure {
            group = TASK_GROUP
            description = "Writes changed projects since a specific commit ref to a file for CI/CD pipeline consumption"
            val customPath = project.findProperty("monorepoBuild.outputFile") as? String
            if (customPath != null) {
                outputFile.set(project.layout.projectDirectory.file(customPath))
            } else {
                outputFile.convention(project.layout.buildDirectory.file("monorepo/changed-projects.txt"))
            }
        }

        project.logger.info("Monorepo Build Plugin applied to ${project.name}")
    }

    /**
     * Determines which detection mode to use based on the tasks requested in this invocation.
     * Fails fast if both branch-mode and ref-mode tasks appear in the same invocation.
     */
    private fun resolveMode(project: Project): DetectionMode {
        val requested = project.gradle.startParameter.taskNames
            .map { it.substringAfterLast(":") }
            .toSet()
        val wantsRef = requested.any { it in REF_TASKS }
        val wantsBranch = requested.any { it in BRANCH_TASKS }
        if (wantsRef && wantsBranch) {
            throw GradleException(
                "Cannot run branch-mode and ref-mode tasks in the same invocation. " +
                "Run printChangedProjectsFromBranch/buildChangedProjectsFromBranch OR " +
                "printChangedProjectsFromRef/buildChangedProjectsFromRef — not both."
            )
        }
        return if (wantsRef) DetectionMode.FROM_REF else DetectionMode.FROM_BRANCH
    }

    /**
     * Resolves the commit ref to use, preferring the project property over the DSL value.
     */
    private fun resolveCommitRef(project: Project, extension: MonorepoBuildExtension): String? {
        val fromProperty = project.findProperty("monorepoBuild.commitRef") as? String
        return (fromProperty ?: extension.commitRef).takeIf { it.isNotBlank() }
    }

    /**
     * Wires dependsOn from a build-aggregation task to the build tasks of all affected projects.
     */
    private fun wireDependsOn(project: Project, taskName: String, affectedProjects: Set<String>) {
        val buildChangedTask = project.tasks.named(taskName)
        affectedProjects.forEach { projectPath ->
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
    }

    /**
     * Computes changed project metadata.
     * Called during the configuration phase to ensure all dependencies are fully resolved.
     *
     * @param project The Gradle project
     * @param extension The plugin extension
     * @param commitRef When non-null, uses two-dot diff against this ref instead of branch comparison
     */
    internal fun computeMetadata(project: Project, extension: MonorepoBuildExtension, commitRef: String? = null) {
        val logger = project.logger

        logger.info("Computing changed project metadata...")
        if (commitRef != null) {
            logger.info("Commit ref: $commitRef")
        } else {
            logger.info("Base branch: ${extension.baseBranch}")
        }
        logger.info("Include untracked: ${extension.includeUntracked}")

        // Initialize detectors and factories
        val gitRepository = GitRepository(project.rootDir, logger)
        val gitDetector = GitChangedFilesDetector(logger, gitRepository)
        val projectMapper = ProjectFileMapper()
        val metadataFactory = ProjectMetadataFactory(logger)

        // Detect changed files from git
        val changedFiles = if (commitRef != null) {
            gitDetector.getChangedFilesFromRef(commitRef, extension.excludePatterns)
        } else {
            gitDetector.getChangedFiles(extension)
        }
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
