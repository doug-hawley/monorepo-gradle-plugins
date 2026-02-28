package io.github.doughawley.monorepobuild

import io.github.doughawley.monorepobuild.domain.MonorepoProjects

/**
 * Formats the changed-projects report as a string.
 *
 * Responsible solely for formatting; callers supply the header and the domain
 * object, and decide how to output the result.
 */
class ChangedProjectsPrinter {

    /**
     * Builds the changed-projects report string.
     *
     * @param header The first line of the report (e.g. "Changed projects:" or "Changed projects (since abc123):")
     * @param monorepoProjects All monorepo projects with their metadata and change information
     * @return The formatted report string ready to be passed to a logger
     */
    fun buildReport(
        header: String,
        monorepoProjects: MonorepoProjects
    ): String {
        val changedProjectPaths = monorepoProjects.getChangedProjectPaths().toSet()
        if (changedProjectPaths.isEmpty()) {
            return "No projects have changed."
        }

        val directlyChangedPaths = monorepoProjects.getProjectsWithDirectChanges()
            .map { it.fullyQualifiedName }
            .toSet()

        val directlyChanged = directlyChangedPaths.sorted()
        val transitivelyAffected = changedProjectPaths
            .filter { it !in directlyChangedPaths }
            .sorted()

        val sb = StringBuilder()
        sb.appendLine(header)
        sb.appendLine()
        directlyChanged.forEach { projectPath ->
            sb.appendLine("  ${displayPath(projectPath)}")
        }

        if (transitivelyAffected.isNotEmpty()) {
            sb.appendLine()
            val maxPathLen = transitivelyAffected.maxOf { displayPath(it).length }
            transitivelyAffected.forEach { projectPath ->
                val via = monorepoProjects.projects.find { it.fullyQualifiedName == projectPath }
                    ?.dependencies
                    ?.filter { it.hasChanges() }
                    ?.map { displayPath(it.fullyQualifiedName) }
                    ?.sorted()
                    ?.joinToString(", ")
                    .orEmpty()
                val annotation = if (via.isNotEmpty()) "  (affected via $via)" else ""
                sb.appendLine("  ${displayPath(projectPath).padEnd(maxPathLen)}$annotation")
            }
        }

        return sb.toString().trimEnd()
    }

    private fun displayPath(path: String): String {
        return if (path == ":") ": (root)" else path
    }
}
