package io.github.doughawley.monorepobuild

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Task that prints which projects have changed based on git history and dependency analysis.
 *
 * This task reads pre-computed metadata from the configuration phase and outputs the results.
 * Metadata computation happens during gradle.afterEvaluate to ensure all project dependencies
 * are fully resolved.
 */
abstract class PrintChangedProjectsTask : DefaultTask() {

    @TaskAction
    fun detectChanges() {
        val extension = project.rootProject.extensions.getByType(MonorepoBuildExtension::class.java)

        // If metadata wasn't computed in configuration phase (e.g., in unit tests),
        // compute it now as a fallback
        if (!extension.metadataComputed) {
            logger.warn("Metadata was not computed in configuration phase. Computing now (this may indicate a test environment).")
            val plugin = project.plugins.findPlugin(MonorepoBuildPlugin::class.java)
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

        // Read pre-computed metadata from configuration phase
        val directlyChangedProjects = extension.changedFilesMap.keys

        val directlyChangedList = if (directlyChangedProjects.isEmpty()) "" else directlyChangedProjects.joinToString(", ")
        logger.lifecycle("Directly changed projects: $directlyChangedList")

        val allAffectedList = if (extension.allAffectedProjects.isEmpty()) "" else extension.allAffectedProjects.joinToString(", ")
        logger.lifecycle("All affected projects (including dependents): $allAffectedList")
    }
}
