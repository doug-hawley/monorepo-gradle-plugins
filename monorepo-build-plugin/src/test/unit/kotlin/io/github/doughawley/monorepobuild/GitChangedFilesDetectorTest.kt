package io.github.doughawley.monorepobuild

import io.kotest.core.spec.style.FunSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk

class GitChangedFilesDetectorTest : FunSpec({

    val gitRepository = mockk<GitRepository>()
    val baseBranchResolver = mockk<BaseBranchResolver>()
    val logger = mockk<org.gradle.api.logging.Logger>(relaxed = true)

    afterEach { clearAllMocks() }

    fun detector() = GitChangedFilesDetector(logger, gitRepository, baseBranchResolver)

    // --- getChangedFiles ---

    test("getChangedFiles returns empty set when not a git repository") {
        // given
        every { gitRepository.isRepository() } returns false

        // when
        val result = detector().getChangedFiles(MonorepoBuildExtension())

        // then
        result.shouldBeEmpty()
    }

    test("getChangedFiles combines branch diff, working tree, staged, and untracked files") {
        // given
        every { gitRepository.isRepository() } returns true
        every { baseBranchResolver.resolve("main") } returns "origin/main"
        every { gitRepository.diffBranch("origin/main") } returns listOf("app/Foo.kt")
        every { gitRepository.workingTreeChanges() } returns listOf("lib/Bar.kt")
        every { gitRepository.stagedFiles() } returns listOf("lib/Baz.kt")
        every { gitRepository.untrackedFiles() } returns listOf("new/Qux.kt")
        val extension = MonorepoBuildExtension().apply { includeUntracked = true }

        // when
        val result = detector().getChangedFiles(extension)

        // then
        result shouldContainAll setOf("app/Foo.kt", "lib/Bar.kt", "lib/Baz.kt", "new/Qux.kt")
    }

    test("getChangedFiles does not include untracked files when includeUntracked is false") {
        // given
        every { gitRepository.isRepository() } returns true
        every { baseBranchResolver.resolve("main") } returns "origin/main"
        every { gitRepository.diffBranch("origin/main") } returns emptyList()
        every { gitRepository.workingTreeChanges() } returns emptyList()
        every { gitRepository.stagedFiles() } returns listOf("staged.kt")
        val extension = MonorepoBuildExtension().apply { includeUntracked = false }

        // when
        val result = detector().getChangedFiles(extension)

        // then
        result shouldContain "staged.kt"
        result.size shouldBe 1
    }

    test("getChangedFiles deduplicates files appearing in multiple sources") {
        // given
        every { gitRepository.isRepository() } returns true
        every { baseBranchResolver.resolve("main") } returns "origin/main"
        every { gitRepository.diffBranch("origin/main") } returns listOf("shared/File.kt")
        every { gitRepository.workingTreeChanges() } returns listOf("shared/File.kt")
        every { gitRepository.stagedFiles() } returns emptyList()
        every { gitRepository.untrackedFiles() } returns emptyList()
        val extension = MonorepoBuildExtension().apply { includeUntracked = true }

        // when
        val result = detector().getChangedFiles(extension)

        // then
        result shouldBe setOf("shared/File.kt")
    }

    test("getChangedFiles applies exclude patterns to filter results") {
        // given
        every { gitRepository.isRepository() } returns true
        every { baseBranchResolver.resolve("main") } returns "origin/main"
        every { gitRepository.diffBranch("origin/main") } returns emptyList()
        every { gitRepository.workingTreeChanges() } returns emptyList()
        every { gitRepository.stagedFiles() } returns listOf("code.kt", "README.md", "docs/guide.md")
        val extension = MonorepoBuildExtension().apply {
            includeUntracked = false
            excludePatterns = listOf(".*\\.md", "docs/.*")
        }

        // when
        val result = detector().getChangedFiles(extension)

        // then
        result shouldContain "code.kt"
        result shouldNotContain "README.md"
        result shouldNotContain "docs/guide.md"
    }

    test("getChangedFiles skips branch comparison when base branch cannot be resolved") {
        // given
        every { gitRepository.isRepository() } returns true
        every { baseBranchResolver.resolve("main") } returns null
        every { gitRepository.workingTreeChanges() } returns emptyList()
        every { gitRepository.stagedFiles() } returns listOf("staged.kt")
        val extension = MonorepoBuildExtension().apply { includeUntracked = false }

        // when
        val result = detector().getChangedFiles(extension)

        // then — staged file is still picked up even though branch diff was skipped
        result shouldContain "staged.kt"
    }

    // --- getChangedFilesFromRef ---

    test("getChangedFilesFromRef returns empty set when not a git repository") {
        // given
        every { gitRepository.isRepository() } returns false

        // when
        val result = detector().getChangedFilesFromRef("abc123", emptyList())

        // then
        result.shouldBeEmpty()
    }

    test("getChangedFilesFromRef returns files changed since the given ref") {
        // given
        every { gitRepository.isRepository() } returns true
        every { gitRepository.diffFromRef("abc123") } returns listOf("module/Foo.kt", "module/Bar.kt")

        // when
        val result = detector().getChangedFilesFromRef("abc123", emptyList())

        // then
        result shouldContainAll setOf("module/Foo.kt", "module/Bar.kt")
    }

    test("getChangedFilesFromRef applies exclude patterns") {
        // given
        every { gitRepository.isRepository() } returns true
        every { gitRepository.diffFromRef("abc123") } returns listOf("code.kt", "README.md")

        // when
        val result = detector().getChangedFilesFromRef("abc123", listOf(".*\\.md"))

        // then
        result shouldContain "code.kt"
        result shouldNotContain "README.md"
    }

    test("getChangedFilesFromRef propagates IllegalArgumentException from diffFromRef") {
        // given
        every { gitRepository.isRepository() } returns true
        every { gitRepository.diffFromRef("bad-ref") } throws
            IllegalArgumentException("Commit ref 'bad-ref' does not exist in this repository.")

        // when / then
        shouldThrow<IllegalArgumentException> {
            detector().getChangedFilesFromRef("bad-ref", emptyList())
        }
    }
})
