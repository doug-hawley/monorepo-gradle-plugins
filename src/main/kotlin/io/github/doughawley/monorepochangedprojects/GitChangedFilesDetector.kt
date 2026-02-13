package io.github.doughawley.monorepochangedprojects

import io.github.doughawley.monorepochangedprojects.git.GitCommandExecutor
import org.gradle.api.logging.Logger
import java.io.File

/**
 * Responsible for detecting changed files from git.
 * Detects committed changes, staged changes, and optionally untracked files.
 */
class GitChangedFilesDetector(private val logger: Logger) {

    private val gitExecutor = GitCommandExecutor(logger)

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
    fun getChangedFiles(rootDir: File, extension: ProjectsChangedExtension): Set<String> {
        val gitDir = findGitRoot(rootDir) ?: run {
            logger.warn("Not a git repository")
            return emptySet()
        }

        val changedFiles = mutableSetOf<String>()

        try {
            // Get changed files compared to base branch (committed changes)
            val branchChanges = getChangedFilesSinceBaseBranch(gitDir, extension.baseBranch)
            logger.lifecycle("Files from branch comparison: ${branchChanges.size}")
            changedFiles.addAll(branchChanges)

            // Also get uncommitted working tree changes (modified files not yet committed)
            val workingTreeChanges = getWorkingTreeChanges(gitDir)
            logger.lifecycle("Working tree changes: ${workingTreeChanges.size}")
            changedFiles.addAll(workingTreeChanges)

            // Get staged files (files added with git add but not yet committed)
            val staged = getStagedFiles(gitDir)
            logger.lifecycle("Staged files: ${staged.size}")
            changedFiles.addAll(staged)

            // Include untracked files if configured
            if (extension.includeUntracked) {
                val untracked = getUntrackedFiles(gitDir)
                logger.lifecycle("Untracked files: ${untracked.size}")
                changedFiles.addAll(untracked)
            }

            logger.lifecycle("Total changed files detected: ${changedFiles.size}")
            if (changedFiles.isNotEmpty()) {
                logger.lifecycle("Changed files: ${changedFiles.take(5).joinToString(", ")}${if (changedFiles.size > 5) "..." else ""}")
            }
        } catch (e: Exception) {
            logger.error("Error executing git command: ${e.message}", e)
        }

        // Apply exclude patterns
        return changedFiles.filterNot { file ->
            extension.excludePatterns.any { pattern ->
                file.matches(Regex(pattern))
            }
        }.toSet()
    }

    private fun getChangedFilesSinceBaseBranch(gitDir: File, baseBranch: String): Set<String> {
        // Compare committed changes between base branch and HEAD
        return try {
            // If baseBranch already includes "origin/", don't add it again
            val branchRef = if (baseBranch.startsWith("origin/")) baseBranch else "origin/$baseBranch"
            gitExecutor.executeForOutput(
                gitDir,
                "diff", "--name-only", "$branchRef...HEAD"
            ).toSet()
        } catch (e: Exception) {
            // If origin/ doesn't work, try local branch comparison
            try {
                gitExecutor.executeForOutput(
                    gitDir,
                    "diff", "--name-only", "$baseBranch...HEAD"
                ).toSet()
            } catch (e2: Exception) {
                logger.warn("Could not compare to branch $baseBranch: ${e2.message}")
                emptySet()
            }
        }
    }

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
        return gitExecutor.executeForOutput(
            gitDir,
            "diff", "--name-only", "--cached"
        ).toSet()
    }

    private fun getUntrackedFiles(gitDir: File): Set<String> {
        return gitExecutor.executeForOutput(
            gitDir,
            "ls-files", "--others", "--exclude-standard"
        ).toSet()
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
