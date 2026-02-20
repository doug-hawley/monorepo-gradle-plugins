package io.github.doughawley.monorepochangedprojects

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Gradle plugin that detects which projects have changed based on git history.
 */
class MonorepoChangedProjectsPlugin : Plugin<Project> {

    companion object {
        private const val COMPUTED_FLAG_KEY = "monorepoChangedProjects.metadataComputed"
    }

    override fun apply(project: Project) {
        // Register the extension on the root project to ensure it's shared
        val rootExtension = if (project == project.rootProject) {
            project.extensions.create(
                "projectsChanged",
                ProjectsChangedExtension::class.java
            )
        } else {
            // If applied to subproject, get or create extension on root
            project.rootProject.extensions.findByType(ProjectsChangedExtension::class.java)
                ?: project.rootProject.extensions.create(
                    "projectsChanged",
                    ProjectsChangedExtension::class.java
                )
        }

        // Compute metadata in configuration phase after ALL projects are evaluated
        // Use gradle.projectsEvaluated to ensure all subprojects are configured
        // Use a flag on the gradle instance to ensure this only runs once
        project.gradle.projectsEvaluated {
            // Synchronize on the root project to guard against parallel project configuration
            // (--parallel) where multiple threads may register and fire this callback
            // concurrently. The check-and-set must be atomic to ensure computeMetadata()
            // runs exactly once.
            synchronized(project.rootProject) {
                val computed = project.gradle.extensions.extraProperties.has(COMPUTED_FLAG_KEY)
                if (!computed) {
                    // Mark as computed before running to prevent re-entry
                    project.gradle.extensions.extraProperties.set(COMPUTED_FLAG_KEY, true)

                    try {
                        computeMetadata(project.rootProject, rootExtension)
                        rootExtension.metadataComputed = true
                        project.logger.debug("Changed project metadata computed successfully in configuration phase")

                        // Wire up dependsOn for each affected project's build task now that we know
                        // which projects changed. This must happen in the configuration phase so
                        // Gradle can include them in the task graph before execution begins.
                        val buildChangedTask = project.tasks.named("buildChangedProjects")
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
        }

        // Register the detectChangedProjects task
        project.tasks.register("detectChangedProjects", DetectChangedProjectsTask::class.java).configure {
            group = "verification"
            description = "Detects which projects have changed based on git history"
        }

        // Register the buildChangedProjects task.
        // Actual dependsOn wiring for affected project build tasks is added dynamically
        // in the projectsEvaluated hook above, after changed projects are known.
        project.tasks.register("buildChangedProjects").configure {
            group = "build"
            description = "Builds only the projects that have been affected by changes"
            dependsOn("detectChangedProjects")

            doLast {
                val extension = project.rootProject.extensions.getByType(ProjectsChangedExtension::class.java)

                if (!extension.metadataComputed) {
                    throw IllegalStateException(
                        "Changed project metadata was not computed in configuration phase. " +
                        "This indicates a plugin initialization error."
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

        project.logger.lifecycle("Projects Changed Plugin applied to ${project.name}")
    }

    /**
     * Computes changed project metadata.
     * Called during task execution to ensure all dependencies are fully resolved.
     *
     * @param project The Gradle project
     * @param extension The plugin extension
     */
    internal fun computeMetadata(project: Project, extension: ProjectsChangedExtension) {
        val logger = project.logger

        logger.lifecycle("Computing changed project metadata...")
        logger.lifecycle("Base branch: ${extension.baseBranch}")
        logger.lifecycle("Include untracked: ${extension.includeUntracked}")

        // Initialize detectors and factories
        val gitDetector = GitChangedFilesDetector(logger)
        val projectMapper = ProjectFileMapper()
        val metadataFactory = ProjectMetadataFactory(logger)

        // Detect changed files from git
        val changedFiles = gitDetector.getChangedFiles(project.rootDir, extension)
        val changedFilesMap = projectMapper.mapChangedFilesToProjects(project.rootProject, changedFiles)

        // Build metadata with changed files information
        val metadataMap = metadataFactory.buildProjectMetadataMap(project.rootProject, changedFilesMap)

        // Get all affected projects (those with changes OR dependency changes)
        val allAffectedProjects = metadataMap.values
            .filter { metadata ->
                metadata.hasChanges() &&
                metadata.fullyQualifiedName != ":" &&
                hasBuildFile(project.rootProject, metadata.fullyQualifiedName)
            }
            .map { it.fullyQualifiedName }
            .toSet()

        // Store in extension for access during configuration and execution
        extension.metadataMap = metadataMap
        extension.allAffectedProjects = allAffectedProjects
        extension.changedFilesMap = changedFilesMap

        logger.lifecycle("Changed files count: ${changedFiles.size}")
        logger.lifecycle("All affected projects (including dependents): ${allAffectedProjects.joinToString(", ").ifEmpty { "none" }}")
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





