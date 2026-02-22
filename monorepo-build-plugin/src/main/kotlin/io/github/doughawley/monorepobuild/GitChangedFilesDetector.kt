package io.github.doughawley.monorepobuild

import io.github.doughawley.monorepobuild.git.GitCommandExecutor
import org.gradle.api.logging.Logger
import java.io.File

/**
 * Responsible for detecting changed files from git.
 * Detects committed changes, staged changes, and optionally untracked files.
 */
class GitChangedFilesDetector(
    private val logger: Logger,
    private val gitExecutor: GitCommandExecutor = GitCommandExecutor(logger)
) {

    /**
     * Gets the list of changed files from git based on the configuration.
     * Includes:
     * - Files changed between base branch and HEAD (committed)
     * - Files staged in the git index (staged but not committed)
     * - Untracked files (if includeUntracked is true)
     *
     * @param rootDir The root directory of the project
     * @param extension The plugin configuration
     * @return Set of changed file paths relative to root
     */
    fun getChangedFiles(rootDir: File, extension: MonorepoBuildExtension): Set<String> {
        val gitDir = findGitRoot(rootDir) ?: run {
            logger.warn("Not a git repository")
            return emptySet()
        }

        val changedFiles = mutableSetOf<String>()

        try {
            // Get changed files compared to base branch (committed changes)
            val branchChanges = getChangedFilesSinceBaseBranch(gitDir, extension.baseBranch)
            logger.info("Files from branch comparison: ${branchChanges.size}")
            changedFiles.addAll(branchChanges)

            // Also get uncommitted working tree changes (modified files not yet committed)
            val workingTreeChanges = getWorkingTreeChanges(gitDir)
            logger.info("Working tree changes: ${workingTreeChanges.size}")
            changedFiles.addAll(workingTreeChanges)

            // Get staged files (files added with git add but not yet committed)
            val staged = getStagedFiles(gitDir)
            logger.info("Staged files: ${staged.size}")
            changedFiles.addAll(staged)

            // Include untracked files if configured
            if (extension.includeUntracked) {
                val untracked = getUntrackedFiles(gitDir)
                logger.info("Untracked files: ${untracked.size}")
                changedFiles.addAll(untracked)
            }

            logger.info("Total changed files detected: ${changedFiles.size}")
            if (changedFiles.isNotEmpty()) {
                logger.info("Changed files: ${changedFiles.take(5).joinToString(", ")}${if (changedFiles.size > 5) "..." else ""}")
            }
        } catch (e: Exception) {
            logger.error("Error executing git command: ${e.message}", e)
        }

        // Pre-compile exclude patterns once rather than recompiling for every file
        val compiledExcludePatterns = extension.excludePatterns.map { Regex(it) }
        return changedFiles.filterNot { file ->
            compiledExcludePatterns.any { pattern -> file.matches(pattern) }
        }.toSet()
    }

    private fun getChangedFilesSinceBaseBranch(gitDir: File, baseBranch: String): Set<String> {
        // Resolve the best available ref before attempting the diff so we can give a
        // clear diagnostic if neither a remote nor a local branch can be found.
        val resolvedRef = resolveBaseBranchRef(gitDir, baseBranch)
        if (resolvedRef == null) {
            logger.warn(
                "Could not resolve base branch '$baseBranch' as a remote (origin/$baseBranch) " +
                "or local ref — skipping branch comparison. " +
                "Check that 'baseBranch' is set correctly in your monorepoBuild configuration."
            )
            return emptySet()
        }

        return try {
            gitExecutor.executeForOutput(
                gitDir,
                "diff", "--name-only", "$resolvedRef...HEAD"
            ).toSet()
        } catch (e: Exception) {
            logger.warn("Could not diff against '$resolvedRef': ${e.message}")
            emptySet()
        }
    }

    /**
     * Resolves the base branch to a concrete git ref that exists in the repository.
     * Preference order:
     *  1. If the caller already supplied a remote ref (e.g. "origin/main"), use it directly.
     *  2. Try the remote tracking ref "origin/<baseBranch>".
     *  3. Fall back to the local branch <baseBranch>.
     * Returns null if none of the candidates exist.
     */
    private fun resolveBaseBranchRef(gitDir: File, baseBranch: String): String? {
        if (baseBranch.startsWith("origin/")) {
            return if (refExists(gitDir, baseBranch)) baseBranch else null
        }
        val remoteRef = "origin/$baseBranch"
        if (refExists(gitDir, remoteRef)) return remoteRef
        if (refExists(gitDir, baseBranch)) return baseBranch
        return null
    }

    private fun refExists(gitDir: File, ref: String): Boolean =
        gitExecutor.execute(gitDir, "rev-parse", "--verify", ref).success

    private fun getWorkingTreeChanges(gitDir: File): Set<String> {
        // Get files modified in working tree but not yet staged or committed
        return try {
            gitExecutor.executeForOutput(
                gitDir,
                "diff", "--name-only", "HEAD"
            ).toSet()
        } catch (e: Exception) {
            logger.warn("Could not get working tree changes: ${e.message}")
            emptySet()
        }
    }

    private fun getStagedFiles(gitDir: File): Set<String> {
        return try {
            gitExecutor.executeForOutput(
                gitDir,
                "diff", "--name-only", "--cached"
            ).toSet()
        } catch (e: Exception) {
            logger.warn("Could not get staged files: ${e.message}")
            emptySet()
        }
    }

    private fun getUntrackedFiles(gitDir: File): Set<String> {
        return try {
            gitExecutor.executeForOutput(
                gitDir,
                "ls-files", "--others", "--exclude-standard"
            ).toSet()
        } catch (e: Exception) {
            logger.warn("Could not get untracked files: ${e.message}")
            emptySet()
        }
    }

    private fun findGitRoot(startDir: File): File? {
        var current: File? = startDir
        while (current != null) {
            if (File(current, ".git").exists()) {
                return current
            }
            current = current.parentFile
        }
        return null
    }
}
