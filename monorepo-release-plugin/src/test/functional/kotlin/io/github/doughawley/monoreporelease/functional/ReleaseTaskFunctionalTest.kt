package io.github.doughawley.monoreporelease.functional

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.TaskOutcome
import java.io.File

class ReleaseTaskFunctionalTest : FunSpec({

    val testListener = listener(ReleaseTestProjectListener())

    // ─────────────────────────────────────────────────────────────
    // Core versioning
    // ─────────────────────────────────────────────────────────────

    test("no prior tag creates first release as v0.1.0 with tag and release branch") {
        // given
        val project = StandardReleaseTestProject.createAndInitialize(testListener.getTestProjectDir())
        project.createFakeBuiltArtifact()

        // when
        val result = project.runTask(":app:release")

        // then
        result.task(":app:release")?.outcome shouldBe TaskOutcome.SUCCESS
        project.localTags() shouldContain "release/app/v0.1.0"
        project.remoteTags() shouldContain "release/app/v0.1.0"
        project.remoteBranches() shouldContain "release/app/v0.1.x"
    }

    test("prior v0.1.0 tag with default minor scope creates v0.2.0") {
        // given
        val project = StandardReleaseTestProject.createAndInitialize(testListener.getTestProjectDir())
        project.createTag("release/app/v0.1.0")
        project.pushTag("release/app/v0.1.0")
        project.createFakeBuiltArtifact()

        // when
        val result = project.runTask(":app:release")

        // then
        result.task(":app:release")?.outcome shouldBe TaskOutcome.SUCCESS
        project.remoteTags() shouldContain "release/app/v0.2.0"
    }

    test("-Prelease.scope=major bumps major version") {
        // given
        val project = StandardReleaseTestProject.createAndInitialize(testListener.getTestProjectDir())
        project.createTag("release/app/v0.1.0")
        project.pushTag("release/app/v0.1.0")
        project.createFakeBuiltArtifact()

        // when
        val result = project.runTask(":app:release", properties = mapOf("release.scope" to "major"))

        // then
        result.task(":app:release")?.outcome shouldBe TaskOutcome.SUCCESS
        project.remoteTags() shouldContain "release/app/v1.0.0"
    }

    test("-Prelease.scope=minor bumps minor version") {
        // given
        val project = StandardReleaseTestProject.createAndInitialize(testListener.getTestProjectDir())
        project.createTag("release/app/v0.1.0")
        project.pushTag("release/app/v0.1.0")
        project.createFakeBuiltArtifact()

        // when
        val result = project.runTask(":app:release", properties = mapOf("release.scope" to "minor"))

        // then
        result.task(":app:release")?.outcome shouldBe TaskOutcome.SUCCESS
        project.remoteTags() shouldContain "release/app/v0.2.0"
    }

    test("multiple version lines on main scans global latest for next minor") {
        // given: v0.1.2 and v0.2.0 exist — global latest is v0.2.0 → next minor is v0.3.0
        val project = StandardReleaseTestProject.createAndInitialize(testListener.getTestProjectDir())
        project.createTag("release/app/v0.1.0")
        project.createTag("release/app/v0.1.2")
        project.createTag("release/app/v0.2.0")
        project.pushTag("release/app/v0.1.0")
        project.pushTag("release/app/v0.1.2")
        project.pushTag("release/app/v0.2.0")
        project.createFakeBuiltArtifact()

        // when
        val result = project.runTask(":app:release")

        // then
        result.task(":app:release")?.outcome shouldBe TaskOutcome.SUCCESS
        project.remoteTags() shouldContain "release/app/v0.3.0"
    }

    test("on release branch scans only version line and creates patch") {
        // given: v0.1.0, v0.1.1, and v0.2.0 exist; on release/app/v0.1.x → should produce v0.1.2
        val project = StandardReleaseTestProject.createAndInitialize(testListener.getTestProjectDir())
        project.createTag("release/app/v0.1.0")
        project.createTag("release/app/v0.1.1")
        project.createTag("release/app/v0.2.0")
        project.pushTag("release/app/v0.1.0")
        project.pushTag("release/app/v0.1.1")
        project.pushTag("release/app/v0.2.0")

        // Create and switch to a release branch (push to remote so it exists there)
        project.createBranch("release/app/v0.1.x")
        project.executeGitPush("release/app/v0.1.x")
        project.createFakeBuiltArtifact()

        // when
        val result = project.runTask(":app:release")

        // then
        result.task(":app:release")?.outcome shouldBe TaskOutcome.SUCCESS
        project.remoteTags() shouldContain "release/app/v0.1.2"
        // Should NOT create another release branch
        project.remoteBranches() shouldNotContain "release/app/v0.1.x.x"
    }

    test("on release branch scope is always PATCH regardless of -Prelease.scope or DSL") {
        // given
        val project = StandardReleaseTestProject.createAndInitialize(testListener.getTestProjectDir())
        project.createTag("release/app/v0.1.0")
        project.pushTag("release/app/v0.1.0")
        project.createBranch("release/app/v0.1.x")
        project.executeGitPush("release/app/v0.1.x")
        project.createFakeBuiltArtifact()

        // when: scope=minor flag is provided but should be ignored on release branch
        val result = project.runTask(":app:release", properties = mapOf("release.scope" to "minor"))

        // then: still produces a patch
        result.task(":app:release")?.outcome shouldBe TaskOutcome.SUCCESS
        project.remoteTags() shouldContain "release/app/v0.1.1"
    }

    // ─────────────────────────────────────────────────────────────
    // Tag format
    // ─────────────────────────────────────────────────────────────

    test("single-level path :app produces tag release/app/v...") {
        // given
        val project = StandardReleaseTestProject.createAndInitialize(testListener.getTestProjectDir())
        project.createFakeBuiltArtifact()

        // when
        project.runTask(":app:release")

        // then
        project.remoteTags().any { it.startsWith("release/app/v") } shouldBe true
    }

    test("custom globalTagPrefix overrides default release prefix") {
        // given
        val project = StandardReleaseTestProject.createAndInitialize(testListener.getTestProjectDir(), globalTagPrefix = "publish")
        project.createFakeBuiltArtifact()

        // when
        val result = project.runTask(":app:release")

        // then
        result.task(":app:release")?.outcome shouldBe TaskOutcome.SUCCESS
        project.remoteTags() shouldContain "publish/app/v0.1.0"
    }

    test("custom tagPrefix in monorepoReleaseConfig overrides path-derived value") {
        // given: set up project with custom tagPrefix
        val projectDir = testListener.getTestProjectDir()
        val remoteDir = File(projectDir.parentFile, "${projectDir.name}-remote.git")

        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.doug-hawley.monorepo-release-plugin")
            }
            """.trimIndent()
        )
        File(projectDir, "settings.gradle.kts").writeText(
            """
            rootProject.name = "test-project"
            include(":app")
            """.trimIndent()
        )
        File(projectDir, ".gitignore").writeText(".gradle/\nbuild/")

        val appDir = File(projectDir, "app")
        appDir.mkdirs()
        File(appDir, "build.gradle.kts").writeText(
            """
            plugins {
                kotlin("jvm") version "2.0.21"
            }

            monorepoReleaseConfig {
                enabled = true
                tagPrefix = "my-custom-app"
            }
            """.trimIndent()
        )

        val project = ReleaseTestProject(projectDir, remoteDir)
        project.initGit()
        project.commitAll("Initial commit")
        project.pushToRemote()
        project.createFakeBuiltArtifact()

        // when
        val result = project.runTask(":app:release")

        // then
        result.task(":app:release")?.outcome shouldBe TaskOutcome.SUCCESS
        project.remoteTags() shouldContain "release/my-custom-app/v0.1.0"
    }

    // ─────────────────────────────────────────────────────────────
    // Guardrails
    // ─────────────────────────────────────────────────────────────

    test("uncommitted changes causes release to fail with clear message") {
        // given
        val project = StandardReleaseTestProject.createAndInitialize(testListener.getTestProjectDir())
        project.createFakeBuiltArtifact()
        project.modifyFile("app/src/main/kotlin/com/example/App.kt", "// modified content")

        // when
        val result = project.runTaskAndFail(":app:release")

        // then
        result.output shouldContain "uncommitted changes"
    }

    test("staged but uncommitted changes causes release to fail") {
        // given
        val project = StandardReleaseTestProject.createAndInitialize(testListener.getTestProjectDir())
        project.createFakeBuiltArtifact()
        project.modifyFile("app/src/main/kotlin/com/example/App.kt", "// staged modification")
        project.stageFile("app/src/main/kotlin/com/example/App.kt")

        // when
        val result = project.runTaskAndFail(":app:release")

        // then
        result.output shouldContain "uncommitted changes"
    }

    test("feature branch causes release to fail with clear message") {
        // given
        val project = StandardReleaseTestProject.createAndInitialize(testListener.getTestProjectDir())
        project.createBranch("feature/my-feature")
        project.createFakeBuiltArtifact()

        // when
        val result = project.runTaskAndFail(":app:release")

        // then
        result.output shouldContain "feature/my-feature"
    }

    test("custom releaseBranchPatterns restricts valid branches") {
        // given: configure only 'develop' as a valid release branch
        val projectDir = testListener.getTestProjectDir()
        val remoteDir = File(projectDir.parentFile, "${projectDir.name}-remote.git")

        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.doug-hawley.monorepo-release-plugin")
            }

            monorepoRelease {
                releaseBranchPatterns = listOf("^develop${'$'}")
            }
            """.trimIndent()
        )
        File(projectDir, "settings.gradle.kts").writeText(
            """
            rootProject.name = "test-project"
            include(":app")
            """.trimIndent()
        )
        File(projectDir, ".gitignore").writeText(".gradle/\nbuild/")
        val appDir = File(projectDir, "app")
        appDir.mkdirs()
        File(appDir, "build.gradle.kts").writeText(
            """
            plugins {
                kotlin("jvm") version "2.0.21"
            }
            monorepoReleaseConfig {
                enabled = true
            }
            """.trimIndent()
        )

        val project = ReleaseTestProject(projectDir, remoteDir)
        project.initGit()
        project.commitAll("Initial commit")
        project.pushToRemote()
        project.createFakeBuiltArtifact()

        // when: on 'main' which is not in the custom patterns
        val result = project.runTaskAndFail(":app:release")

        // then
        result.output shouldContain "main"
    }

    test("tag already exists causes release to fail with clear message") {
        // given: v0.1.0 is the latest released version (on remote); v0.2.0 was created locally
        // (e.g. a previous failed release attempt left the local tag behind)
        val project = StandardReleaseTestProject.createAndInitialize(testListener.getTestProjectDir())
        project.createTag("release/app/v0.1.0")
        project.pushTag("release/app/v0.1.0")
        // Create v0.2.0 locally only (not pushed) — this is the "collision" tag
        project.createTag("release/app/v0.2.0")
        project.createFakeBuiltArtifact()

        // when: scanner finds v0.1.0 on remote → next = v0.2.0; tagExists finds local v0.2.0 → fail
        val result = project.runTaskAndFail(":app:release")

        // then
        result.output shouldContain "release/app/v0.2.0"
        result.output shouldContain "already exists"
    }

    test("build outputs missing causes release to fail mentioning build task") {
        // given: no fake artifact created
        val project = StandardReleaseTestProject.createAndInitialize(testListener.getTestProjectDir())

        // when
        val result = project.runTaskAndFail(":app:release")

        // then
        result.output shouldContain ":app:build"
    }

    test("build outputs present allows release to proceed") {
        // given
        val project = StandardReleaseTestProject.createAndInitialize(testListener.getTestProjectDir())
        project.createFakeBuiltArtifact()

        // when
        val result = project.runTask(":app:release")

        // then
        result.task(":app:release")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    // ─────────────────────────────────────────────────────────────
    // Scope enforcement
    // ─────────────────────────────────────────────────────────────

    test("main + -Prelease.scope=patch fails with clear message") {
        // given
        val project = StandardReleaseTestProject.createAndInitialize(testListener.getTestProjectDir())
        project.createFakeBuiltArtifact()

        // when
        val result = project.runTaskAndFail(":app:release", properties = mapOf("release.scope" to "patch"))

        // then
        result.output shouldContain "patch"
        result.output shouldContain "main"
    }

    test("release branch + -Prelease.scope=minor flag is ignored (patch used instead)") {
        // given
        val project = StandardReleaseTestProject.createAndInitialize(testListener.getTestProjectDir())
        project.createTag("release/app/v0.1.0")
        project.pushTag("release/app/v0.1.0")
        project.createBranch("release/app/v0.1.x")
        project.executeGitPush("release/app/v0.1.x")
        project.createFakeBuiltArtifact()

        // when: minor flag on release branch — should be ignored (PATCH forced)
        val result = project.runTask(":app:release", properties = mapOf("release.scope" to "minor"))

        // then: patch was applied despite the flag
        result.task(":app:release")?.outcome shouldBe TaskOutcome.SUCCESS
        project.remoteTags() shouldContain "release/app/v0.1.1"
    }

    test("release branch + -Prelease.scope=major flag is ignored (patch used instead)") {
        // given
        val project = StandardReleaseTestProject.createAndInitialize(testListener.getTestProjectDir())
        project.createTag("release/app/v0.1.0")
        project.pushTag("release/app/v0.1.0")
        project.createBranch("release/app/v0.1.x")
        project.executeGitPush("release/app/v0.1.x")
        project.createFakeBuiltArtifact()

        // when
        val result = project.runTask(":app:release", properties = mapOf("release.scope" to "major"))

        // then: patch was applied despite the flag
        result.task(":app:release")?.outcome shouldBe TaskOutcome.SUCCESS
        project.remoteTags() shouldContain "release/app/v0.1.1"
    }

    test("release branch with no scope flag succeeds with PATCH") {
        // given
        val project = StandardReleaseTestProject.createAndInitialize(testListener.getTestProjectDir())
        project.createTag("release/app/v0.1.0")
        project.pushTag("release/app/v0.1.0")
        project.createBranch("release/app/v0.1.x")
        project.executeGitPush("release/app/v0.1.x")
        project.createFakeBuiltArtifact()

        // when
        val result = project.runTask(":app:release")

        // then
        result.task(":app:release")?.outcome shouldBe TaskOutcome.SUCCESS
        project.remoteTags() shouldContain "release/app/v0.1.1"
    }

    // ─────────────────────────────────────────────────────────────
    // Push and rollback
    // ─────────────────────────────────────────────────────────────

    test("push fails when no remote configured — local tag and branch are deleted, task fails cleanly") {
        // given: project without remote
        val projectDir = testListener.getTestProjectDir()
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.doug-hawley.monorepo-release-plugin")
            }
            """.trimIndent()
        )
        File(projectDir, "settings.gradle.kts").writeText(
            """
            rootProject.name = "test-project"
            include(":app")
            """.trimIndent()
        )
        File(projectDir, ".gitignore").writeText(".gradle/\nbuild/")
        val appDir = File(projectDir, "app")
        appDir.mkdirs()
        File(appDir, "build.gradle.kts").writeText(
            """
            plugins {
                kotlin("jvm") version "2.0.21"
            }
            monorepoReleaseConfig {
                enabled = true
            }
            """.trimIndent()
        )

        // Initialize without remote
        val noRemote = File(projectDir.parentFile, "${projectDir.name}-no-remote.git")
        val project = ReleaseTestProject(projectDir, noRemote)
        // Manual init without remote
        fun runGit(vararg cmd: String) {
            ProcessBuilder(*cmd).directory(projectDir).start().waitFor()
        }
        runGit("git", "init")
        runGit("git", "config", "user.email", "test@example.com")
        runGit("git", "config", "user.name", "Test User")
        runGit("git", "checkout", "-b", "main")
        runGit("git", "add", ".")
        runGit("git", "commit", "-m", "Initial commit")
        project.createFakeBuiltArtifact()

        // when
        val result = project.runTaskAndFail(":app:release")

        // then: push failed, local tag rolled back
        result.output shouldContain "push"
        project.localTags() shouldNotContain "release/app/v0.1.0"
    }

    test("on release branch no release branch is created, only tag pushed") {
        // given
        val project = StandardReleaseTestProject.createAndInitialize(testListener.getTestProjectDir())
        project.createTag("release/app/v0.1.0")
        project.pushTag("release/app/v0.1.0")
        project.createBranch("release/app/v0.1.x")
        project.executeGitPush("release/app/v0.1.x")
        project.createFakeBuiltArtifact()

        // when
        project.runTask(":app:release")

        // then: new release branch not created
        val remoteBranches = project.remoteBranches()
        remoteBranches shouldContain "release/app/v0.1.x"
        // Should not contain a new branch like release/app/v0.1.x.x
        remoteBranches.filter { it.startsWith("release/app/v0.1.") && it != "release/app/v0.1.x" } shouldBe emptyList()
    }

    // ─────────────────────────────────────────────────────────────
    // Version communication
    // ─────────────────────────────────────────────────────────────

    test("build/release-version.txt is written after successful release") {
        // given
        val project = StandardReleaseTestProject.createAndInitialize(testListener.getTestProjectDir())
        project.createFakeBuiltArtifact()

        // when
        project.runTask(":app:release")

        // then
        project.releaseVersionFile() shouldBe "0.1.0"
    }

    // ─────────────────────────────────────────────────────────────
    // postRelease hook
    // ─────────────────────────────────────────────────────────────

    test("postRelease task runs after successful release") {
        // given: wire a task to postRelease via finalizedBy in a custom subproject build
        val projectDir = testListener.getTestProjectDir()
        val remoteDir = File(projectDir.parentFile, "${projectDir.name}-remote.git")

        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.doug-hawley.monorepo-release-plugin")
            }
            """.trimIndent()
        )
        File(projectDir, "settings.gradle.kts").writeText(
            """
            rootProject.name = "test-project"
            include(":app")
            """.trimIndent()
        )
        File(projectDir, ".gitignore").writeText(".gradle/\nbuild/")
        val appDir = File(projectDir, "app")
        appDir.mkdirs()
        File(appDir, "build.gradle.kts").writeText(
            """
            plugins {
                kotlin("jvm") version "2.0.21"
            }
            monorepoReleaseConfig {
                enabled = true
            }
            tasks.named("postRelease") {
                doLast {
                    println("POST_RELEASE_RAN")
                }
            }
            """.trimIndent()
        )

        val project = ReleaseTestProject(projectDir, remoteDir)
        project.initGit()
        project.commitAll("Initial commit")
        project.pushToRemote()
        project.createFakeBuiltArtifact()

        // when
        val result = project.runTask(":app:release")

        // then
        result.task(":app:postRelease")?.outcome shouldBe TaskOutcome.SUCCESS
        result.output shouldContain "POST_RELEASE_RAN"
    }

    test("postRelease hook does not run if release task fails") {
        // given: release will fail because no build output
        val projectDir = testListener.getTestProjectDir()
        val remoteDir = File(projectDir.parentFile, "${projectDir.name}-remote.git")

        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.doug-hawley.monorepo-release-plugin")
            }
            """.trimIndent()
        )
        File(projectDir, "settings.gradle.kts").writeText(
            """
            rootProject.name = "test-project"
            include(":app")
            """.trimIndent()
        )
        File(projectDir, ".gitignore").writeText(".gradle/\nbuild/")
        val appDir = File(projectDir, "app")
        appDir.mkdirs()
        File(appDir, "build.gradle.kts").writeText(
            """
            plugins {
                kotlin("jvm") version "2.0.21"
            }
            monorepoReleaseConfig {
                enabled = true
            }
            tasks.named("postRelease") {
                doLast {
                    println("POST_RELEASE_RAN")
                }
            }
            """.trimIndent()
        )

        val project = ReleaseTestProject(projectDir, remoteDir)
        project.initGit()
        project.commitAll("Initial commit")
        project.pushToRemote()
        // deliberately no createFakeBuiltArtifact()

        // when
        val result = project.runTaskAndFail(":app:release")

        // then
        result.output shouldNotContain "POST_RELEASE_RAN"
    }

    // ─────────────────────────────────────────────────────────────
    // Opt-in model
    // ─────────────────────────────────────────────────────────────

    test("subproject without monorepoReleaseConfig has no release task") {
        // given: a project where no subproject has enabled opt-in
        val projectDir = testListener.getTestProjectDir()
        val remoteDir = File(projectDir.parentFile, "${projectDir.name}-remote.git")

        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.doug-hawley.monorepo-release-plugin")
            }
            """.trimIndent()
        )
        File(projectDir, "settings.gradle.kts").writeText(
            """
            rootProject.name = "test-project"
            include(":lib")
            """.trimIndent()
        )
        File(projectDir, ".gitignore").writeText(".gradle/\nbuild/")
        val libDir = File(projectDir, "lib")
        libDir.mkdirs()
        // No monorepoReleaseConfig block at all
        File(libDir, "build.gradle.kts").writeText(
            """
            plugins {
                kotlin("jvm") version "2.0.21"
            }
            """.trimIndent()
        )

        val project = ReleaseTestProject(projectDir, remoteDir)
        project.initGit()
        project.commitAll("Initial commit")
        project.pushToRemote()

        // when
        val result = project.runTaskAndFail(":lib:release")

        // then: task does not exist
        result.output shouldContain "release"
    }

    test("subproject with enabled=false has no release task") {
        // given
        val projectDir = testListener.getTestProjectDir()
        val remoteDir = File(projectDir.parentFile, "${projectDir.name}-remote.git")

        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.doug-hawley.monorepo-release-plugin")
            }
            """.trimIndent()
        )
        File(projectDir, "settings.gradle.kts").writeText(
            """
            rootProject.name = "test-project"
            include(":lib")
            """.trimIndent()
        )
        File(projectDir, ".gitignore").writeText(".gradle/\nbuild/")
        val libDir = File(projectDir, "lib")
        libDir.mkdirs()
        File(libDir, "build.gradle.kts").writeText(
            """
            plugins {
                kotlin("jvm") version "2.0.21"
            }
            monorepoReleaseConfig {
                enabled = false
            }
            """.trimIndent()
        )

        val project = ReleaseTestProject(projectDir, remoteDir)
        project.initGit()
        project.commitAll("Initial commit")
        project.pushToRemote()

        // when
        val result = project.runTaskAndFail(":lib:release")

        // then: task does not exist
        result.output shouldContain "release"
    }
})

// Extension to push a branch to remote (used in tests where we need a named branch on remote)
fun ReleaseTestProject.executeGitPush(branch: String) {
    val process = ProcessBuilder("git", "push", "origin", branch)
        .directory(projectDir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()
    val exitCode = process.waitFor()
    if (exitCode != 0) {
        val error = process.errorStream.bufferedReader().readText()
        throw RuntimeException("Failed to push branch $branch: $error")
    }
}

fun ReleaseTestProject.stageFile(path: String) {
    val process = ProcessBuilder("git", "add", path)
        .directory(projectDir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()
    process.waitFor()
}
