package io.github.doughawley.monorepobuild

import io.github.doughawley.monorepobuild.domain.ProjectMetadata
import org.gradle.api.Project

/**
 * Formats the changed-projects report as a string.
 *
 * Responsible solely for formatting; callers supply the header and the three
 * data values needed to render the report, and decide how to output the result.
 */
class ChangedProjectsPrinter(private val rootProject: Project) {

    /**
     * Builds the changed-projects report string.
     *
     * @param header The first line of the report (e.g. "Changed projects:" or "Changed projects (since abc123):")
     * @param allAffectedProjects All project paths affected by the change (directly or transitively)
     * @param changedFilesMap Map of project path to the files that changed directly in that project
     * @param metadataMap Map of project path to its full metadata (used to resolve transitive "via" annotations)
     * @return The formatted report string ready to be passed to a logger
     */
    fun buildReport(
        header: String,
        allAffectedProjects: Set<String>,
        changedFilesMap: Map<String, List<String>>,
        metadataMap: Map<String, ProjectMetadata>
    ): String {
        if (allAffectedProjects.isEmpty()) {
            return "No projects have changed."
        }

        val directlyChanged = changedFilesMap.keys
            .filter { it in allAffectedProjects }
            .sorted()

        val transitivelyAffected = allAffectedProjects
            .filter { it !in changedFilesMap.keys }
            .sorted()

        val sb = StringBuilder()
        sb.appendLine(header)

        directlyChanged.forEach { projectPath ->
            sb.appendLine()
            sb.appendLine("  $projectPath")
            val files = buildDisplayFiles(projectPath, changedFilesMap)
            files.take(FILE_DISPLAY_LIMIT).forEach { sb.appendLine("    - $it") }
            if (files.size > FILE_DISPLAY_LIMIT) {
                sb.appendLine("    ... and ${files.size - FILE_DISPLAY_LIMIT} more")
            }
        }

        if (transitivelyAffected.isNotEmpty()) {
            sb.appendLine()
            val maxPathLen = transitivelyAffected.maxOf { it.length }
            transitivelyAffected.forEach { projectPath ->
                val via = metadataMap[projectPath]
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

        return sb.toString().trimEnd()
    }

    private fun buildDisplayFiles(projectPath: String, changedFilesMap: Map<String, List<String>>): List<String> {
        val files = changedFilesMap[projectPath].orEmpty()
        val projectDir = rootProject.findProject(projectPath)
            ?.projectDir
            ?.relativeTo(rootProject.rootDir)
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
        const val FILE_DISPLAY_LIMIT = 50
    }
}
