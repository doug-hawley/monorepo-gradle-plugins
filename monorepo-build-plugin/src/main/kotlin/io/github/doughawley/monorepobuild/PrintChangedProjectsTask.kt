package io.github.doughawley.monorepobuild

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Task that prints which projects have changed based on git history and dependency analysis.
 *
 * Output format:
 *   - Directly changed projects are listed with their changed files (relative to the project directory).
 *   - Transitively affected projects are listed with an "(affected via ...)" annotation naming
 *     the direct dependencies that carry the change.
 *   - File lists are capped at FILE_DISPLAY_LIMIT entries.
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
        sb.appendLine("Changed projects:")

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
