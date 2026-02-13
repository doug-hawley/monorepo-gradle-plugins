package io.github.doughawley.monorepochangedprojects

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.gradle.testfixtures.ProjectBuilder

class ProjectFileMapperTest : FunSpec({

    test("should identify root project when files in root directory change") {
        // given
        val rootProject = ProjectBuilder.builder().build()
        val subproject = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("submodule")
            .build()
        val mapper = ProjectFileMapper()
        val changedFiles = setOf("build.gradle.kts", "README.md")

        // when
        val result = mapper.findProjectsWithChangedFiles(rootProject, changedFiles)

        // then
        result shouldContain ":"
        result.size shouldBe 1
    }

    test("should identify subproject when files in subproject directory change") {
        // given
        val rootProject = ProjectBuilder.builder().build()
        val subproject = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("submodule")
            .build()
        val mapper = ProjectFileMapper()
        val changedFiles = setOf("submodule/src/main/kotlin/App.kt")

        // when
        val result = mapper.findProjectsWithChangedFiles(rootProject, changedFiles)

        // then
        result shouldContain ":submodule"
    }

    test("should return empty set when no files match any project") {
        // given
        val rootProject = ProjectBuilder.builder().build()
        val mapper = ProjectFileMapper()
        val changedFiles = emptySet<String>()

        // when
        val result = mapper.findProjectsWithChangedFiles(rootProject, changedFiles)

        // then
        result.shouldBeEmpty()
    }

    test("should handle multiple changed files in different projects") {
        // given
        val rootProject = ProjectBuilder.builder().build()
        val sub1 = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("module-a")
            .build()
        val sub2 = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("module-b")
            .build()
        val mapper = ProjectFileMapper()
        val changedFiles = setOf(
            "module-a/src/main/kotlin/App.kt",
            "module-b/src/test/kotlin/Test.kt"
        )

        // when
        val result = mapper.findProjectsWithChangedFiles(rootProject, changedFiles)

        // then
        result.size shouldBe 2
        result shouldContain ":module-a"
        result shouldContain ":module-b"
    }
})
