package io.github.doughawley.monorepochangedprojects

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Task that detects which projects have changed based on git history and dependency analysis.
 *
 * This task reads pre-computed metadata from the configuration phase and outputs the results.
 * Metadata computation happens during gradle.afterEvaluate to ensure all project dependencies
 * are fully resolved.
 */
abstract class DetectChangedProjectsTask : DefaultTask() {

    @TaskAction
    fun detectChanges() {
        val extension = project.rootProject.extensions.getByType(ProjectsChangedExtension::class.java)

        // If metadata wasn't computed in configuration phase (e.g., in unit tests),
        // compute it now as a fallback
        if (!extension.metadataComputed) {
            logger.warn("Metadata was not computed in configuration phase. Computing now (this may indicate a test environment).")
            val plugin = project.plugins.findPlugin(MonorepoChangedProjectsPlugin::class.java)
            if (plugin != null) {
                plugin.computeMetadata(project.rootProject, extension)
                extension.metadataComputed = true
            } else {
                throw IllegalStateException(
                    "Changed project metadata was not computed and plugin instance not found. " +
                    "This indicates a plugin initialization error."
                )
            }
        }

        logger.lifecycle("Detecting changed projects...")
        logger.lifecycle("Base branch: ${extension.baseBranch}")
        logger.lifecycle("Include untracked: ${extension.includeUntracked}")

        // Read pre-computed metadata from configuration phase
        val metadataMap = extension.metadataMap
        val allAffectedProjects = extension.allAffectedProjects
        val changedFilesMap = extension.changedFilesMap
        val directlyChangedProjects = changedFilesMap.keys

        // Count total changed files
        val totalChangedFiles = changedFilesMap.values.flatten().toSet().size

        logger.lifecycle("Changed files count: $totalChangedFiles")

        val directlyChangedList = if (directlyChangedProjects.isEmpty()) "" else directlyChangedProjects.joinToString(", ")
        logger.lifecycle("Directly changed projects: $directlyChangedList")

        val allAffectedList = if (allAffectedProjects.isEmpty()) "" else allAffectedProjects.joinToString(", ")
        logger.lifecycle("All affected projects (including dependents): $allAffectedList")

        if (allAffectedProjects.isEmpty()) {
            logger.lifecycle("No projects have changed")
        }

        // Store results in project extra properties for backward compatibility
        project.extensions.extraProperties.set("changedProjects", allAffectedProjects)
        project.extensions.extraProperties.set("changedProjectsMetadata", metadataMap)
        project.extensions.extraProperties.set("changedFilesMap", changedFilesMap)
    }
}
