package com.bitmoxie.monorepochangedprojects

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
            changedFiles.addAll(getChangedFilesSinceBaseBranch(gitDir, extension.baseBranch))

            // Get staged files (files added with git add but not yet committed)
            changedFiles.addAll(getStagedFiles(gitDir))

            // Include untracked files if configured
            if (extension.includeUntracked) {
                changedFiles.addAll(getUntrackedFiles(gitDir))
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
        return gitExecutor.executeForOutput(
            gitDir,
            "diff", "--name-only", "origin/$baseBranch...HEAD"
        ).toSet()
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
