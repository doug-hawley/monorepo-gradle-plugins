package io.github.doughawley.monorepochangedprojects

/**
 * Extension for configuring the monorepo-changed-projects plugin.
 */
open class ProjectsChangedExtension {
    /**
     * The base branch to compare against (default: main)
     */
    var baseBranch: String = "main"

    /**
     * Whether to include untracked files in the change detection
     */
    var includeUntracked: Boolean = true

    /**
     * File patterns to exclude from change detection
     */
    var excludePatterns: List<String> = listOf()
}
