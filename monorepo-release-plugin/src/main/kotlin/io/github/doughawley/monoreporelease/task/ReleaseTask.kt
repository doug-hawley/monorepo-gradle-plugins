package io.github.doughawley.monoreporelease.task

import io.github.doughawley.monoreporelease.MonorepoReleaseConfigExtension
import io.github.doughawley.monoreporelease.MonorepoReleaseExtension
import io.github.doughawley.monoreporelease.domain.Scope
import io.github.doughawley.monoreporelease.domain.SemanticVersion
import io.github.doughawley.monoreporelease.domain.TagPattern
import io.github.doughawley.monoreporelease.git.GitReleaseExecutor
import io.github.doughawley.monoreporelease.git.GitTagScanner
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class ReleaseTask : DefaultTask() {

    @get:Internal
    lateinit var rootExtension: MonorepoReleaseExtension

    @get:Internal
    lateinit var projectConfig: MonorepoReleaseConfigExtension

    @get:Internal
    lateinit var gitTagScanner: GitTagScanner

    @get:Internal
    lateinit var gitReleaseExecutor: GitReleaseExecutor

    @TaskAction
    fun release() {
        // 1. Opt-in check
        if (!projectConfig.enabled) {
            throw GradleException(
                "Release is not enabled for ${project.path}. " +
                "Set monorepoReleaseConfig { enabled = true } to opt in."
            )
        }

        // 2. Dirty check
        if (gitReleaseExecutor.isDirty()) {
            throw GradleException(
                "Cannot release with uncommitted changes. " +
                "Please commit or stash all changes before releasing."
            )
        }

        // 3. Branch validation
        val globalPrefix = rootExtension.globalTagPrefix
        val currentBranch = gitReleaseExecutor.currentBranch()
        val isReleaseBranch = TagPattern.isReleaseBranch(currentBranch, globalPrefix)
        val isAllowedBranch = isReleaseBranch || rootExtension.releaseBranchPatterns.any { pattern ->
            currentBranch.matches(Regex(pattern))
        }
        if (!isAllowedBranch) {
            throw GradleException(
                "Cannot release from branch '$currentBranch'. " +
                "Releases must be made from a configured release branch. " +
                "Allowed patterns: ${rootExtension.releaseBranchPatterns.joinToString(", ")}"
            )
        }

        // 4. Scope resolution
        val scope = resolveScope(isReleaseBranch)

        // 5. Determine tag prefix
        val projectPrefix = projectConfig.tagPrefix
            ?: TagPattern.deriveProjectTagPrefix(project.path)

        // 6. Scan tags to find next version
        val latestVersion = if (isReleaseBranch) {
            val (major, minor) = TagPattern.parseVersionLineFromBranch(currentBranch)
            gitTagScanner.findLatestVersionInLine(globalPrefix, projectPrefix, major, minor)
        } else {
            gitTagScanner.findLatestVersion(globalPrefix, projectPrefix)
        }

        val nextVersion = if (latestVersion == null) {
            SemanticVersion(0, 1, 0)
        } else {
            latestVersion.bump(scope)
        }

        // 7. Tag collision check
        val tag = TagPattern.formatTag(globalPrefix, projectPrefix, nextVersion)
        if (gitTagScanner.tagExists(tag)) {
            throw GradleException(
                "Tag '$tag' already exists. " +
                "This version has already been released."
            )
        }

        // 8. Build outputs check
        val libsDir = project.layout.buildDirectory.dir("libs").get().asFile
        if (!libsDir.exists() || libsDir.listFiles()?.isEmpty() != false) {
            throw GradleException(
                "Project must be built before releasing — run ${project.path}:build first."
            )
        }

        // 9. Set project.version
        project.version = nextVersion.toString()
        logger.lifecycle("Releasing ${project.path} as version $nextVersion")

        // 10. Create tag locally
        gitReleaseExecutor.createTagLocally(tag)

        // 11. Create release branch locally (only on main)
        val releaseBranch = if (isMainBranch(currentBranch)) {
            val branch = TagPattern.formatReleaseBranch(globalPrefix, projectPrefix, nextVersion)
            gitReleaseExecutor.createBranchLocally(branch)
            branch
        } else {
            null
        }

        // 12. Push to remote (with rollback on failure)
        try {
            gitReleaseExecutor.pushTagAndBranch(tag, releaseBranch)
        } catch (e: GradleException) {
            logger.error("Push failed, rolling back local changes: ${e.message}")
            gitReleaseExecutor.deleteLocalTag(tag)
            if (releaseBranch != null) {
                gitReleaseExecutor.deleteLocalBranch(releaseBranch)
            }
            throw e
        }

        // 13. Write build/release-version.txt
        val versionFile = project.layout.buildDirectory.file("release-version.txt").get().asFile
        versionFile.parentFile.mkdirs()
        versionFile.writeText(nextVersion.toString())
        logger.lifecycle("Wrote release version to: ${versionFile.absolutePath}")
    }

    private fun resolveScope(isReleaseBranch: Boolean): Scope {
        if (isReleaseBranch) {
            return Scope.PATCH
        }

        val scopeProperty = project.findProperty("release.scope") as? String
        if (scopeProperty != null) {
            val parsed = Scope.fromString(scopeProperty)
                ?: throw GradleException(
                    "Invalid release.scope value: '$scopeProperty'. " +
                    "Must be one of: major, minor, patch"
                )
            if (parsed == Scope.PATCH) {
                throw GradleException(
                    "Cannot use scope 'patch' on the main branch. " +
                    "Use 'minor' or 'major' for new feature releases."
                )
            }
            return parsed
        }

        val dslScope = Scope.fromString(rootExtension.releaseChangedProjectsScope)
            ?: throw GradleException(
                "Invalid releaseChangedProjectsScope in monorepoRelease DSL: " +
                "'${rootExtension.releaseChangedProjectsScope}'. " +
                "Must be one of: major, minor"
            )
        if (dslScope == Scope.PATCH) {
            throw GradleException(
                "Cannot configure releaseChangedProjectsScope as 'patch' on the main branch. " +
                "Use 'minor' or 'major'."
            )
        }
        return dslScope
    }

    private fun isMainBranch(branch: String): Boolean {
        return branch == "main" || branch == "master"
    }
}
