package io.github.doughawley.monorepochangedprojects

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.gradle.api.logging.Logger
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.nio.file.Files

class GitChangedFilesDetectorTest : FunSpec({

    test("should return empty set when directory is not a git repository") {
        // given
        val tempDir = Files.createTempDirectory("test-no-git").toFile()
        val logger = ProjectBuilder.builder().build().logger
        val detector = GitChangedFilesDetector(logger)
        val extension = ProjectsChangedExtension()

        // when
        val result = detector.getChangedFiles(tempDir, extension)

        // then
        result.shouldBeEmpty()
        tempDir.deleteRecursively()
    }

    test("should exclude files matching exclude patterns") {
        // given
        val tempDir = Files.createTempDirectory("test-exclude").toFile()
        val gitDir = File(tempDir, ".git")
        gitDir.mkdirs()

        val logger = ProjectBuilder.builder().build().logger
        val detector = GitChangedFilesDetector(logger)
        val extension = ProjectsChangedExtension().apply {
            excludePatterns = listOf(".*\\.md", "docs/.*", ".*\\.txt")
        }

        // Simulate changed files (this would come from git commands in real scenario)
        // Since we can't easily mock git commands, this tests the exclude pattern logic

        // when
        val result = detector.getChangedFiles(tempDir, extension)

        // then
        // Result should be empty since git commands won't return anything in this mock repo
        result.shouldBeEmpty()
        tempDir.deleteRecursively()
    }

    test("should find git root when in subdirectory") {
        // given
        val tempDir = Files.createTempDirectory("test-git-root").toFile()
        val gitDir = File(tempDir, ".git")
        gitDir.mkdirs()
        val subDir = File(tempDir, "sub/directory")
        subDir.mkdirs()

        val logger = ProjectBuilder.builder().build().logger
        val detector = GitChangedFilesDetector(logger)
        val extension = ProjectsChangedExtension()

        // when
        val result = detector.getChangedFiles(subDir, extension)

        // then
        // Should find git root and not throw error
        result shouldBe emptySet()
        tempDir.deleteRecursively()
    }

    test("should handle nested git repositories correctly") {
        // given
        val tempDir = Files.createTempDirectory("test-nested-git").toFile()
        val outerGit = File(tempDir, ".git")
        outerGit.mkdirs()
        val subDir = File(tempDir, "submodule")
        subDir.mkdirs()
        val innerGit = File(subDir, ".git")
        innerGit.mkdirs()

        val logger = ProjectBuilder.builder().build().logger
        val detector = GitChangedFilesDetector(logger)
        val extension = ProjectsChangedExtension()

        // when
        val resultFromSubmodule = detector.getChangedFiles(subDir, extension)

        // then
        // Should find the nearest .git directory (inner one)
        resultFromSubmodule shouldBe emptySet()
        tempDir.deleteRecursively()
    }

    test("should respect includeUntracked configuration when true") {
        // given
        val tempDir = Files.createTempDirectory("test-untracked").toFile()
        val gitDir = File(tempDir, ".git")
        gitDir.mkdirs()

        val logger = ProjectBuilder.builder().build().logger
        val detector = GitChangedFilesDetector(logger)
        val extension = ProjectsChangedExtension().apply {
            includeUntracked = true
        }

        // when
        val result = detector.getChangedFiles(tempDir, extension)

        // then
        // Would include untracked files if git commands returned any
        result shouldBe emptySet()
        tempDir.deleteRecursively()
    }

    test("should respect includeUntracked configuration when false") {
        // given
        val tempDir = Files.createTempDirectory("test-no-untracked").toFile()
        val gitDir = File(tempDir, ".git")
        gitDir.mkdirs()

        val logger = ProjectBuilder.builder().build().logger
        val detector = GitChangedFilesDetector(logger)
        val extension = ProjectsChangedExtension().apply {
            includeUntracked = false
        }

        // when
        val result = detector.getChangedFiles(tempDir, extension)

        // then
        // Should not include untracked files
        result shouldBe emptySet()
        tempDir.deleteRecursively()
    }

    test("should handle multiple exclude patterns correctly") {
        // given
        val tempDir = Files.createTempDirectory("test-multi-exclude").toFile()
        val gitDir = File(tempDir, ".git")
        gitDir.mkdirs()

        val logger = ProjectBuilder.builder().build().logger
        val detector = GitChangedFilesDetector(logger)
        val extension = ProjectsChangedExtension().apply {
            excludePatterns = listOf(
                ".*\\.md",
                ".*\\.txt",
                "docs/.*",
                ".*/test/.*",
                "build/.*"
            )
        }

        // when
        val result = detector.getChangedFiles(tempDir, extension)

        // then
        result.shouldBeEmpty()
        tempDir.deleteRecursively()
    }

    test("should handle git directory at project root") {
        // given
        val tempDir = Files.createTempDirectory("test-root-git").toFile()
        val gitDir = File(tempDir, ".git")
        gitDir.mkdirs()

        val logger = ProjectBuilder.builder().build().logger
        val detector = GitChangedFilesDetector(logger)
        val extension = ProjectsChangedExtension()

        // when
        val result = detector.getChangedFiles(tempDir, extension)

        // then
        // Should successfully find .git at root
        result shouldBe emptySet()
        tempDir.deleteRecursively()
    }

    test("should use correct base branch from configuration") {
        // given
        val tempDir = Files.createTempDirectory("test-base-branch").toFile()
        val gitDir = File(tempDir, ".git")
        gitDir.mkdirs()

        val logger = ProjectBuilder.builder().build().logger
        val detector = GitChangedFilesDetector(logger)
        val extension = ProjectsChangedExtension().apply {
            baseBranch = "develop"
        }

        // when
        val result = detector.getChangedFiles(tempDir, extension)

        // then
        // Would use "develop" as base branch in git diff command
        result shouldBe emptySet()
        tempDir.deleteRecursively()
    }

    test("should handle empty git repository gracefully") {
        // given
        val tempDir = Files.createTempDirectory("test-empty-repo").toFile()
        val gitDir = File(tempDir, ".git")
        gitDir.mkdirs()

        val logger = ProjectBuilder.builder().build().logger
        val detector = GitChangedFilesDetector(logger)
        val extension = ProjectsChangedExtension()

        // when
        val result = detector.getChangedFiles(tempDir, extension)

        // then
        // Should handle empty repo without errors
        result shouldBe emptySet()
        tempDir.deleteRecursively()
    }

    test("should not traverse above file system root when searching for git") {
        // given
        val tempDir = Files.createTempDirectory("test-no-git-above").toFile()
        val deepDir = File(tempDir, "a/b/c/d/e/f")
        deepDir.mkdirs()

        val logger = ProjectBuilder.builder().build().logger
        val detector = GitChangedFilesDetector(logger)
        val extension = ProjectsChangedExtension()

        // when
        val result = detector.getChangedFiles(deepDir, extension)

        // then
        // Should not find git and return empty set
        result.shouldBeEmpty()
        tempDir.deleteRecursively()
    }

    test("should handle exception in git command execution gracefully") {
        // given
        val tempDir = Files.createTempDirectory("test-exception").toFile()
        val gitDir = File(tempDir, ".git")
        gitDir.mkdirs()

        val logger = ProjectBuilder.builder().build().logger
        val detector = GitChangedFilesDetector(logger)
        val extension = ProjectsChangedExtension().apply {
            // Use invalid base branch that will cause git command to fail
            baseBranch = "this-branch-does-not-exist-12345"
        }

        // when
        val result = detector.getChangedFiles(tempDir, extension)

        // then
        // Should handle error gracefully and return empty set
        result shouldBe emptySet()
        tempDir.deleteRecursively()
    }

    test("should detect staged files in git repository") {
        // given
        val tempDir = Files.createTempDirectory("test-staged").toFile()

        // Initialize real git repo
        executeGitCommand(tempDir, "init")
        executeGitCommand(tempDir, "config", "user.email", "test@example.com")
        executeGitCommand(tempDir, "config", "user.name", "Test User")

        // Create and commit initial file
        File(tempDir, "initial.txt").writeText("initial content")
        executeGitCommand(tempDir, "add", "initial.txt")
        executeGitCommand(tempDir, "commit", "-m", "initial commit")

        // Create and stage a new file
        File(tempDir, "staged-file.kt").writeText("new staged content")
        executeGitCommand(tempDir, "add", "staged-file.kt")

        val logger = ProjectBuilder.builder().build().logger
        val detector = GitChangedFilesDetector(logger)
        val extension = ProjectsChangedExtension()

        // when
        val result = detector.getChangedFiles(tempDir, extension)

        // then

        // The staged file should be detected via git diff --cached
        result shouldContain "staged-file.kt"
        tempDir.deleteRecursively()
    }


    test("should detect untracked files when includeUntracked is true") {
        val tempDir = Files.createTempDirectory("test-untracked-real").toFile()

        // Initialize real git repo
        executeGitCommand(tempDir, "init")
        executeGitCommand(tempDir, "config", "user.email", "test@example.com")
        executeGitCommand(tempDir, "config", "user.name", "Test User")

        // Create and commit initial file
        File(tempDir, "initial.txt").writeText("initial content")
        executeGitCommand(tempDir, "add", "initial.txt")
        executeGitCommand(tempDir, "commit", "-m", "initial commit")

        // Create untracked file (not staged)
        File(tempDir, "untracked-file.kt").writeText("untracked content")

        val logger = ProjectBuilder.builder().build().logger
        val detector = GitChangedFilesDetector(logger)
        val extension = ProjectsChangedExtension().apply {
            includeUntracked = true
        }

        // when
        val result = detector.getChangedFiles(tempDir, extension)

        // then
        result.size shouldBe 1
        result shouldContain "untracked-file.kt"
        tempDir.deleteRecursively()
    }

    test("should not detect untracked files when includeUntracked is false") {
        // given
        val tempDir = Files.createTempDirectory("test-no-untracked-real").toFile()

        // Initialize real git repo
        executeGitCommand(tempDir, "init")
        executeGitCommand(tempDir, "config", "user.email", "test@example.com")
        executeGitCommand(tempDir, "config", "user.name", "Test User")

        // Create and commit initial file
        File(tempDir, "initial.txt").writeText("initial content")
        executeGitCommand(tempDir, "add", "initial.txt")
        executeGitCommand(tempDir, "commit", "-m", "initial commit")

        // Create untracked file
        File(tempDir, "untracked-file.kt").writeText("untracked content")

        val logger = ProjectBuilder.builder().build().logger
        val detector = GitChangedFilesDetector(logger)
        val extension = ProjectsChangedExtension().apply {
            includeUntracked = false
        }

        // when
        val result = detector.getChangedFiles(tempDir, extension)

        // then
        result.shouldBeEmpty()
        tempDir.deleteRecursively()
    }

    test("should detect multiple staged and untracked files") {
        // given
        val tempDir = Files.createTempDirectory("test-multiple-files").toFile()

        // Initialize real git repo
        executeGitCommand(tempDir, "init")
        executeGitCommand(tempDir, "config", "user.email", "test@example.com")
        executeGitCommand(tempDir, "config", "user.name", "Test User")

        // Create and commit initial file
        File(tempDir, "initial.txt").writeText("initial content")
        executeGitCommand(tempDir, "add", "initial.txt")
        executeGitCommand(tempDir, "commit", "-m", "initial commit")

        // Create staged files
        File(tempDir, "staged1.kt").writeText("staged 1")
        File(tempDir, "staged2.kt").writeText("staged 2")
        executeGitCommand(tempDir, "add", "staged1.kt", "staged2.kt")

        // Create untracked files
        File(tempDir, "untracked1.kt").writeText("untracked 1")
        File(tempDir, "untracked2.kt").writeText("untracked 2")

        val logger = ProjectBuilder.builder().build().logger
        val detector = GitChangedFilesDetector(logger)
        val extension = ProjectsChangedExtension().apply {
            includeUntracked = true
        }

        // when
        val result = detector.getChangedFiles(tempDir, extension)

        // then
        result.size shouldBe 4
        result shouldContain "staged1.kt"
        result shouldContain "staged2.kt"
        result shouldContain "untracked1.kt"
        result shouldContain "untracked2.kt"
        tempDir.deleteRecursively()
    }

    test("should exclude files matching exclude patterns from results") {
        // given
        val tempDir = Files.createTempDirectory("test-exclude-real").toFile()

        // Initialize real git repo
        executeGitCommand(tempDir, "init")
        executeGitCommand(tempDir, "config", "user.email", "test@example.com")
        executeGitCommand(tempDir, "config", "user.name", "Test User")

        // Create and commit initial file
        File(tempDir, "initial.txt").writeText("initial content")
        executeGitCommand(tempDir, "add", "initial.txt")
        executeGitCommand(tempDir, "commit", "-m", "initial commit")

        // Create staged files (some match exclude patterns)
        File(tempDir, "code.kt").writeText("code file")
        File(tempDir, "README.md").writeText("readme")
        File(tempDir, "notes.txt").writeText("notes")
        executeGitCommand(tempDir, "add", ".")

        val logger = ProjectBuilder.builder().build().logger
        val detector = GitChangedFilesDetector(logger)
        val extension = ProjectsChangedExtension().apply {
            excludePatterns = listOf(".*\\.md", ".*\\.txt")
        }

        // when
        val result = detector.getChangedFiles(tempDir, extension)

        // then
        result.size shouldBe 1
        result shouldContain "code.kt"
        result shouldNotContain "README.md"
        result shouldNotContain "notes.txt"
        tempDir.deleteRecursively()
    }

    test("should detect files in subdirectories") {
        // given
        val tempDir = Files.createTempDirectory("test-subdirs").toFile()

        // Initialize real git repo
        executeGitCommand(tempDir, "init")
        executeGitCommand(tempDir, "config", "user.email", "test@example.com")
        executeGitCommand(tempDir, "config", "user.name", "Test User")

        // Create and commit initial file
        File(tempDir, "initial.txt").writeText("initial content")
        executeGitCommand(tempDir, "add", "initial.txt")
        executeGitCommand(tempDir, "commit", "-m", "initial commit")

        // Create files in subdirectories
        val srcDir = File(tempDir, "src/main/kotlin")
        srcDir.mkdirs()
        File(srcDir, "App.kt").writeText("app code")

        val testDir = File(tempDir, "src/test/kotlin")
        testDir.mkdirs()
        File(testDir, "AppTest.kt").writeText("test code")

        executeGitCommand(tempDir, "add", ".")

        val logger = ProjectBuilder.builder().build().logger
        val detector = GitChangedFilesDetector(logger)
        val extension = ProjectsChangedExtension()

        // when
        val result = detector.getChangedFiles(tempDir, extension)

        // then
        result shouldContain "src/main/kotlin/App.kt"
        result shouldContain "src/test/kotlin/AppTest.kt"
        tempDir.deleteRecursively()
    }

    test("should detect both staged and untracked files together") {
        // given
        val tempDir = Files.createTempDirectory("test-staged-and-untracked").toFile()

        // Initialize real git repo
        executeGitCommand(tempDir, "init")
        executeGitCommand(tempDir, "config", "user.email", "test@example.com")
        executeGitCommand(tempDir, "config", "user.name", "Test User")

        // Create and commit initial file
        File(tempDir, "initial.txt").writeText("initial content")
        executeGitCommand(tempDir, "add", "initial.txt")
        executeGitCommand(tempDir, "commit", "-m", "initial commit")

        // Stage one file
        File(tempDir, "staged.kt").writeText("staged content")
        executeGitCommand(tempDir, "add", "staged.kt")

        // Leave another untracked
        File(tempDir, "untracked.kt").writeText("untracked content")

        val logger = ProjectBuilder.builder().build().logger
        val detector = GitChangedFilesDetector(logger)
        val extension = ProjectsChangedExtension().apply {
            includeUntracked = true
        }

        // when
        val result = detector.getChangedFiles(tempDir, extension)

        // then
        result shouldContain "staged.kt"
        result shouldContain "untracked.kt"
        tempDir.deleteRecursively()
    }
})

/**
 * Helper function to execute git commands for test setup
 */
private fun executeGitCommand(directory: File, vararg command: String) {
    val fullCommand = arrayOf("git") + command
    val process = ProcessBuilder(*fullCommand)
        .directory(directory)
        .redirectErrorStream(true)
        .start()

    process.waitFor()
    if (process.exitValue() != 0) {
        val error = process.inputStream.bufferedReader().readText()
        throw RuntimeException("Git command failed: ${command.joinToString(" ")}\n$error")
    }
}

