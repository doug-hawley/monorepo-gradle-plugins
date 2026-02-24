package io.github.doughawley.monorepobuild

import org.gradle.api.logging.Logger

/**
 * Resolves a base branch name to a concrete git ref that exists in the repository.
 *
 * Preference order:
 *  1. If the caller already supplied a remote ref (e.g. "origin/main"), use it directly.
 *  2. Try the remote tracking ref "origin/<baseBranch>".
 *  3. Fall back to the local branch <baseBranch>.
 *
 * Returns null if none of the candidates exist, allowing the caller to degrade gracefully.
 */
class BaseBranchResolver(
    private val logger: Logger,
    private val gitRepository: GitRepository
) {

    fun resolve(baseBranch: String): String? {
        if (baseBranch.startsWith("origin/")) {
            return if (gitRepository.refExists(baseBranch)) baseBranch else null
        }
        val remoteRef = "origin/$baseBranch"
        if (gitRepository.refExists(remoteRef)) return remoteRef
        if (gitRepository.refExists(baseBranch)) return baseBranch
        return null
    }
}
