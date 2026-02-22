package io.github.doughawley.monorepobuild

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
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

    test("file in nested subproject should map only to deepest project, not parent hierarchy node") {
        // given
        val rootProject = ProjectBuilder.builder().build()
        val appsNode = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("apps")
            .build()
        val _app1 = ProjectBuilder.builder()
            .withParent(appsNode)
            .withName("app1")
            .build()
        val mapper = ProjectFileMapper()
        val changedFiles = setOf("apps/app1/src/main/kotlin/App.kt")

        // when
        val result = mapper.findProjectsWithChangedFiles(rootProject, changedFiles)

        // then
        result shouldContain ":apps:app1"
        result shouldNotContain ":apps"
        result.size shouldBe 1
    }

    test("sibling projects under hierarchy node each map to their own project only") {
        // given
        val rootProject = ProjectBuilder.builder().build()
        val appsNode = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("apps")
            .build()
        val _app1 = ProjectBuilder.builder()
            .withParent(appsNode)
            .withName("app1")
            .build()
        val _app2 = ProjectBuilder.builder()
            .withParent(appsNode)
            .withName("app2")
            .build()
        val mapper = ProjectFileMapper()
        val changedFiles = setOf(
            "apps/app1/src/main/kotlin/App1.kt",
            "apps/app2/src/main/kotlin/App2.kt"
        )

        // when
        val result = mapper.findProjectsWithChangedFiles(rootProject, changedFiles)

        // then
        result shouldContain ":apps:app1"
        result shouldContain ":apps:app2"
        result shouldNotContain ":apps"
        result.size shouldBe 2
    }

    test("hierarchy nodes across multiple parent directories do not appear") {
        // given
        val rootProject = ProjectBuilder.builder().build()
        val appsNode = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("apps")
            .build()
        val modulesNode = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("modules")
            .build()
        val _app1 = ProjectBuilder.builder()
            .withParent(appsNode)
            .withName("app1")
            .build()
        val _module1 = ProjectBuilder.builder()
            .withParent(modulesNode)
            .withName("module1")
            .build()
        val mapper = ProjectFileMapper()
        val changedFiles = setOf(
            "apps/app1/src/main/kotlin/App.kt",
            "modules/module1/src/main/kotlin/Module.kt"
        )

        // when
        val result = mapper.findProjectsWithChangedFiles(rootProject, changedFiles)

        // then
        result shouldContain ":apps:app1"
        result shouldContain ":modules:module1"
        result shouldNotContain ":apps"
        result shouldNotContain ":modules"
        result.size shouldBe 2
    }

    test("mapChangedFilesToProjects assigns each file to exactly one project") {
        // given
        val rootProject = ProjectBuilder.builder().build()
        val appsNode = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("apps")
            .build()
        val _app1 = ProjectBuilder.builder()
            .withParent(appsNode)
            .withName("app1")
            .build()
        val mapper = ProjectFileMapper()
        val changedFiles = setOf(
            "apps/app1/src/main/kotlin/App.kt",
            "apps/app1/src/test/kotlin/AppTest.kt"
        )

        // when
        val result = mapper.mapChangedFilesToProjects(rootProject, changedFiles)

        // then
        result.keys shouldContain ":apps:app1"
        result.keys shouldNotContain ":apps"
        result[":apps:app1"]?.size shouldBe 2
    }
})
