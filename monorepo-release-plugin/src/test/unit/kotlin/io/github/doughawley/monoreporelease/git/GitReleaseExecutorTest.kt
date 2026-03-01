package io.github.doughawley.monoreporelease.git

import io.github.doughawley.monorepocore.git.GitCommandExecutor
import io.github.doughawley.monorepocore.git.GitCommandExecutor.CommandResult
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import java.io.File

class GitReleaseExecutorTest : FunSpec({

    val executor = mockk<GitCommandExecutor>()
    val logger = mockk<Logger>(relaxed = true)
    val rootDir = File("/fake/root")
    val releaseExecutor = GitReleaseExecutor(rootDir, executor, logger)

    afterEach { clearAllMocks() }

    // isDirty

    test("isDirty returns false when working tree is clean") {
        // given
        every { executor.execute(rootDir, "status", "--porcelain") } returns
            CommandResult(success = true, output = emptyList(), exitCode = 0)

        // when / then
        releaseExecutor.isDirty() shouldBe false
    }

    test("isDirty returns true when there are uncommitted changes") {
        // given
        every { executor.execute(rootDir, "status", "--porcelain") } returns
            CommandResult(success = true, output = listOf(" M src/Main.kt"), exitCode = 0)

        // when / then
        releaseExecutor.isDirty() shouldBe true
    }

    // currentBranch

    test("currentBranch returns the branch name from git output") {
        // given
        every { executor.execute(rootDir, "rev-parse", "--abbrev-ref", "HEAD") } returns
            CommandResult(success = true, output = listOf("main"), exitCode = 0)

        // when / then
        releaseExecutor.currentBranch() shouldBe "main"
    }

    test("currentBranch trims whitespace from output") {
        // given
        every { executor.execute(rootDir, "rev-parse", "--abbrev-ref", "HEAD") } returns
            CommandResult(success = true, output = listOf("  release/app/v1.0.x  "), exitCode = 0)

        // when / then
        releaseExecutor.currentBranch() shouldBe "release/app/v1.0.x"
    }

    test("currentBranch throws GradleException when git command fails") {
        // given
        every { executor.execute(rootDir, "rev-parse", "--abbrev-ref", "HEAD") } returns
            CommandResult(success = false, output = emptyList(), exitCode = 128, errorOutput = "fatal: not a git repo")

        // when / then
        val ex = shouldThrow<GradleException> { releaseExecutor.currentBranch() }
        ex.message shouldContain "Failed to determine current git branch"
    }

    test("currentBranch throws GradleException when output is empty despite success") {
        // given
        every { executor.execute(rootDir, "rev-parse", "--abbrev-ref", "HEAD") } returns
            CommandResult(success = true, output = emptyList(), exitCode = 0)

        // when / then
        shouldThrow<GradleException> { releaseExecutor.currentBranch() }
    }

    // createTagLocally

    test("createTagLocally succeeds when git tag command exits zero") {
        // given
        every { executor.execute(rootDir, "tag", "release/app/v1.0.0") } returns
            CommandResult(success = true, output = emptyList(), exitCode = 0)

        // when / then — no exception
        releaseExecutor.createTagLocally("release/app/v1.0.0")
    }

    test("createTagLocally throws GradleException when git tag command fails") {
        // given
        every { executor.execute(rootDir, "tag", "release/app/v1.0.0") } returns
            CommandResult(success = false, output = emptyList(), exitCode = 128, errorOutput = "tag already exists")

        // when / then
        val ex = shouldThrow<GradleException> { releaseExecutor.createTagLocally("release/app/v1.0.0") }
        ex.message shouldContain "Failed to create local tag"
        ex.message shouldContain "release/app/v1.0.0"
    }

    // createBranchLocally

    test("createBranchLocally succeeds when git branch command exits zero") {
        // given
        every { executor.execute(rootDir, "branch", "release/app/v1.0.x") } returns
            CommandResult(success = true, output = emptyList(), exitCode = 0)

        // when / then — no exception
        releaseExecutor.createBranchLocally("release/app/v1.0.x")
    }

    test("createBranchLocally throws GradleException when git branch command fails") {
        // given
        every { executor.execute(rootDir, "branch", "release/app/v1.0.x") } returns
            CommandResult(success = false, output = emptyList(), exitCode = 128, errorOutput = "branch already exists")

        // when / then
        val ex = shouldThrow<GradleException> { releaseExecutor.createBranchLocally("release/app/v1.0.x") }
        ex.message shouldContain "Failed to create local branch"
        ex.message shouldContain "release/app/v1.0.x"
    }

    // pushTagAndBranch

    test("pushTagAndBranch pushes only the tag when branch is null") {
        // given
        every { executor.execute(rootDir, "push", "origin", "release/app/v1.0.0") } returns
            CommandResult(success = true, output = emptyList(), exitCode = 0)

        // when
        releaseExecutor.pushTagAndBranch("release/app/v1.0.0", null)

        // then
        verify { executor.execute(rootDir, "push", "origin", "release/app/v1.0.0") }
    }

    test("pushTagAndBranch pushes tag and branch in a single command") {
        // given
        every {
            executor.execute(rootDir, "push", "origin", "release/app/v1.0.0", "release/app/v1.0.x")
        } returns CommandResult(success = true, output = emptyList(), exitCode = 0)

        // when
        releaseExecutor.pushTagAndBranch("release/app/v1.0.0", "release/app/v1.0.x")

        // then
        verify { executor.execute(rootDir, "push", "origin", "release/app/v1.0.0", "release/app/v1.0.x") }
    }

    test("pushTagAndBranch throws GradleException when push fails") {
        // given
        every { executor.execute(rootDir, "push", "origin", "release/app/v1.0.0") } returns
            CommandResult(success = false, output = emptyList(), exitCode = 1, errorOutput = "remote rejected")

        // when / then
        val ex = shouldThrow<GradleException> { releaseExecutor.pushTagAndBranch("release/app/v1.0.0", null) }
        ex.message shouldContain "Failed to push to remote"
    }

    // deleteLocalTag

    test("deleteLocalTag succeeds without throwing") {
        // given
        every { executor.execute(rootDir, "tag", "-d", "release/app/v1.0.0") } returns
            CommandResult(success = true, output = emptyList(), exitCode = 0)

        // when / then — no exception
        releaseExecutor.deleteLocalTag("release/app/v1.0.0")
    }

    test("deleteLocalTag logs a warning and does not throw when deletion fails") {
        // given
        every { executor.execute(rootDir, "tag", "-d", "release/app/v1.0.0") } returns
            CommandResult(success = false, output = emptyList(), exitCode = 1, errorOutput = "tag not found")

        // when / then — no exception
        releaseExecutor.deleteLocalTag("release/app/v1.0.0")
        verify { logger.warn(any()) }
    }

    // deleteLocalBranch

    test("deleteLocalBranch succeeds without throwing") {
        // given
        every { executor.execute(rootDir, "branch", "-D", "release/app/v1.0.x") } returns
            CommandResult(success = true, output = emptyList(), exitCode = 0)

        // when / then — no exception
        releaseExecutor.deleteLocalBranch("release/app/v1.0.x")
    }

    test("deleteLocalBranch logs a warning and does not throw when deletion fails") {
        // given
        every { executor.execute(rootDir, "branch", "-D", "release/app/v1.0.x") } returns
            CommandResult(success = false, output = emptyList(), exitCode = 1, errorOutput = "branch not found")

        // when / then — no exception
        releaseExecutor.deleteLocalBranch("release/app/v1.0.x")
        verify { logger.warn(any()) }
    }
})
