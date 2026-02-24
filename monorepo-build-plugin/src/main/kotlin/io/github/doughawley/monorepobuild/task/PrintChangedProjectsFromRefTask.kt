package io.github.doughawley.monorepobuild.task

import io.github.doughawley.monorepobuild.ChangedProjectsPrinter
import io.github.doughawley.monorepobuild.MonorepoBuildExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

/**
 * Task that prints which projects have changed since a specific commit ref.
 *
 * Output format:
 *   - Directly changed projects are listed with their changed files (relative to the project directory).
 *   - Transitively affected projects are listed with an "(affected via ...)" annotation naming
 *     the direct dependencies that carry the change.
 *   - File lists are capped at ChangedProjectsPrinter.FILE_DISPLAY_LIMIT entries.
 */
abstract class PrintChangedProjectsFromRefTask : DefaultTask() {

    @TaskAction
    fun detectChanges() {
        val extension = project.rootProject.extensions.getByType(MonorepoBuildExtension::class.java)

        if (!extension.metadataComputed) {
            throw GradleException(
                "Changed project metadata was not computed in the configuration phase. " +
                "Possible causes: the plugin was not applied to the root project, " +
                "or an error occurred during project evaluation. " +
                "Re-run with --info or --debug for more details."
            )
        }

        val resolvedRef = extension.commitRef
        logger.lifecycle(ChangedProjectsPrinter(project.rootProject).buildReport(
            header = "Changed projects (since $resolvedRef):",
            allAffectedProjects = extension.allAffectedProjects,
            changedFilesMap = extension.changedFilesMap,
            metadataMap = extension.metadataMap
        ))
    }
}
