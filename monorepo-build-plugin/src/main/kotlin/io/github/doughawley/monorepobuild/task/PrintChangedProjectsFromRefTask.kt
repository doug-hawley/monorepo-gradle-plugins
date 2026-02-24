package io.github.doughawley.monorepobuild.task

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
 *   - File lists are capped at FILE_DISPLAY_LIMIT entries.
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
        val allAffected = extension.allAffectedProjects

        if (allAffected.isEmpty()) {
            logger.lifecycle("No projects have changed.")
            return
        }

        val directlyChanged = extension.changedFilesMap.keys
            .filter { it in allAffected }
            .sorted()

        val transitivelyAffected = allAffected
            .filter { it !in extension.changedFilesMap.keys }
            .sorted()

        val sb = StringBuilder()
        sb.appendLine("Changed projects (since $resolvedRef):")

        directlyChanged.forEach { projectPath ->
            sb.appendLine()
            sb.appendLine("  $projectPath")
            val files = buildDisplayFiles(projectPath, extension.changedFilesMap)
            files.take(FILE_DISPLAY_LIMIT).forEach { sb.appendLine("    - $it") }
            if (files.size > FILE_DISPLAY_LIMIT) {
                sb.appendLine("    ... and ${files.size - FILE_DISPLAY_LIMIT} more")
            }
        }

        if (transitivelyAffected.isNotEmpty()) {
            sb.appendLine()
            val maxPathLen = transitivelyAffected.maxOf { it.length }
            transitivelyAffected.forEach { projectPath ->
                val via = extension.metadataMap[projectPath]
                    ?.dependencies
                    ?.filter { it.hasChanges() }
                    ?.map { it.fullyQualifiedName }
                    ?.sorted()
                    ?.joinToString(", ")
                    .orEmpty()
                val annotation = if (via.isNotEmpty()) "  (affected via $via)" else ""
                sb.appendLine("  ${projectPath.padEnd(maxPathLen)}$annotation")
            }
        }

        logger.lifecycle(sb.toString().trimEnd())
    }

    private fun buildDisplayFiles(projectPath: String, changedFilesMap: Map<String, List<String>>): List<String> {
        val files = changedFilesMap[projectPath].orEmpty()
        val projectDir = project.rootProject.findProject(projectPath)
            ?.projectDir
            ?.relativeTo(project.rootProject.rootDir)
            ?.path
            ?.replace('\\', '/')
            .orEmpty()
        return files.map { file ->
            if (projectDir.isNotEmpty() && file.startsWith("$projectDir/")) {
                file.removePrefix("$projectDir/")
            } else {
                file
            }
        }.sorted()
    }

    companion object {
        internal const val FILE_DISPLAY_LIMIT = 50
    }
}
