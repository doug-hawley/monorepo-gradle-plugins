package io.github.doughawley.monorepobuild

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk

class BaseBranchResolverTest : FunSpec({

    val gitRepository = mockk<GitRepository>()
    val logger = mockk<org.gradle.api.logging.Logger>(relaxed = true)

    afterEach { clearAllMocks() }

    test("returns baseBranch directly when it already starts with origin/ and the ref exists") {
        // given
        every { gitRepository.refExists("origin/main") } returns true
        val resolver = BaseBranchResolver(logger, gitRepository)

        // when
        val result = resolver.resolve("origin/main")

        // then
        result shouldBe "origin/main"
    }

    test("returns null when baseBranch starts with origin/ but the ref does not exist") {
        // given
        every { gitRepository.refExists("origin/nonexistent-branch") } returns false
        val resolver = BaseBranchResolver(logger, gitRepository)

        // when
        val result = resolver.resolve("origin/nonexistent-branch")

        // then
        result shouldBe null
    }

    test("returns remote tracking ref when origin/<baseBranch> exists") {
        // given
        every { gitRepository.refExists("origin/develop") } returns true
        val resolver = BaseBranchResolver(logger, gitRepository)

        // when
        val result = resolver.resolve("develop")

        // then
        result shouldBe "origin/develop"
    }

    test("falls back to local branch when remote tracking ref does not exist") {
        // given
        every { gitRepository.refExists("origin/feature-x") } returns false
        every { gitRepository.refExists("feature-x") } returns true
        val resolver = BaseBranchResolver(logger, gitRepository)

        // when
        val result = resolver.resolve("feature-x")

        // then
        result shouldBe "feature-x"
    }

    test("returns null when neither remote tracking ref nor local branch exists") {
        // given
        every { gitRepository.refExists("origin/ghost-branch") } returns false
        every { gitRepository.refExists("ghost-branch") } returns false
        val resolver = BaseBranchResolver(logger, gitRepository)

        // when
        val result = resolver.resolve("ghost-branch")

        // then
        result shouldBe null
    }

    test("prefers remote tracking ref over local branch when both exist") {
        // given
        every { gitRepository.refExists("origin/shared") } returns true
        val resolver = BaseBranchResolver(logger, gitRepository)

        // when
        val result = resolver.resolve("shared")

        // then — remote tracking ref is checked first and returned immediately
        result shouldBe "origin/shared"
    }
})
