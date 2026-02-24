package io.github.doughawley.monorepobuild

import io.github.doughawley.monorepobuild.domain.ProjectMetadata
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Extension for configuring the monorepo-build plugin.
 */
open class MonorepoBuildExtension {
    /**
     * The base branch to compare against when detecting changed files.
     * Defaults to "main" — override this if your repository uses a different
     * primary branch name (e.g. "master", "develop", "trunk").
     *
     * Supports both local branch names (e.g. "main") and remote refs
     * (e.g. "origin/main"). A local name is automatically prefixed with
     * "origin/" before falling back to a local-only comparison.
     */
    var baseBranch: String = "main"

    /**
     * An explicit commit ref (SHA, tag, or ref expression) to compare against HEAD
     * when using ref-mode tasks (printChangedProjectsFromRef, buildChangedProjectsFromRef,
     * writeChangedProjectsFromRef). Defaults to "HEAD~1" so ref-mode tasks work out of the
     * box for single-commit pipelines. Can also be supplied at runtime via
     * -PmonorepoBuild.commitRef=<sha>, which takes precedence over this value.
     */
    var commitRef: String = "HEAD~1"

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
    var metadataMap: Map<String, ProjectMetadata> = emptyMap()
        internal set

    /**
     * Set of all affected project paths (including those affected by dependency changes).
     * Available after configuration phase completes.
     */
    var allAffectedProjects: Set<String> = emptySet()
        internal set

    /**
     * Map of changed files by project path.
     * Available after configuration phase completes.
     */
    var changedFilesMap: Map<String, List<String>> = emptyMap()
        internal set

    /**
     * Guards against concurrent metadata computation under --parallel builds.
     * The first thread to win compareAndSet(false, true) performs the computation;
     * all others see true and skip it.
     */
    internal val computationGuard = AtomicBoolean(false)

    /**
     * Flag indicating whether metadata has been computed in the configuration phase.
     * Marked volatile to ensure the write is visible to all threads once set.
     * Used to ensure metadata is available before task execution.
     */
    @Volatile
    internal var metadataComputed: Boolean = false
}
