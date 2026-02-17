package io.github.doughawley.monorepochangedprojects

import io.github.doughawley.monorepochangedprojects.domain.ProjectMetadata

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

    /**
     * Metadata map computed during configuration phase.
     * Maps project paths to their metadata including dependencies and changed files.
     * Available after configuration phase completes.
     */
    internal var metadataMap: Map<String, ProjectMetadata> = emptyMap()

    /**
     * Set of all affected project paths (including those affected by dependency changes).
     * Available after configuration phase completes.
     */
    internal var allAffectedProjects: Set<String> = emptySet()

    /**
     * Map of changed files by project path.
     * Available after configuration phase completes.
     */
    internal var changedFilesMap: Map<String, List<String>> = emptyMap()

    /**
     * Flag indicating whether metadata has been computed in the configuration phase.
     * Used to ensure metadata is available before task execution.
     */
    internal var metadataComputed: Boolean = false
}
