package io.github.doughawley.monorepobuild

import io.github.doughawley.monorepobuild.domain.ProjectMetadata
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testfixtures.ProjectBuilder

class ChangedProjectsPrinterTest : FunSpec({

    test("returns no-changes message when no projects are affected") {
        // given
        val rootProject = ProjectBuilder.builder().build()
        val printer = ChangedProjectsPrinter(rootProject)

        // when
        val result = printer.buildReport(
            header = "Changed projects:",
            allAffectedProjects = emptySet(),
            changedFilesMap = emptyMap(),
            metadataMap = emptyMap()
        )

        // then
        result shouldBe "No projects have changed."
    }

    test("includes custom header in the output") {
        // given
        val rootProject = ProjectBuilder.builder().build()
        ProjectBuilder.builder().withParent(rootProject).withName("api").build()
        val printer = ChangedProjectsPrinter(rootProject)

        // when
        val result = printer.buildReport(
            header = "Changed projects (since abc123):",
            allAffectedProjects = setOf(":api"),
            changedFilesMap = mapOf(":api" to listOf("api/src/main/kotlin/Api.kt")),
            metadataMap = mapOf(":api" to ProjectMetadata("api", ":api", changedFiles = listOf("api/src/main/kotlin/Api.kt")))
        )

        // then
        result shouldContain "Changed projects (since abc123):"
    }

    test("lists directly changed project and its files") {
        // given
        val rootProject = ProjectBuilder.builder().build()
        ProjectBuilder.builder().withParent(rootProject).withName("api").build()
        val printer = ChangedProjectsPrinter(rootProject)

        // when
        val result = printer.buildReport(
            header = "Changed projects:",
            allAffectedProjects = setOf(":api"),
            changedFilesMap = mapOf(":api" to listOf("api/src/main/kotlin/Api.kt")),
            metadataMap = mapOf(":api" to ProjectMetadata("api", ":api", changedFiles = listOf("api/src/main/kotlin/Api.kt")))
        )

        // then
        result shouldContain ":api"
        result shouldContain "src/main/kotlin/Api.kt"
    }

    test("strips project directory prefix from file paths") {
        // given
        val rootProject = ProjectBuilder.builder().build()
        ProjectBuilder.builder().withParent(rootProject).withName("api").build()
        val printer = ChangedProjectsPrinter(rootProject)

        // when
        val result = printer.buildReport(
            header = "Changed projects:",
            allAffectedProjects = setOf(":api"),
            changedFilesMap = mapOf(":api" to listOf("api/src/main/kotlin/Api.kt")),
            metadataMap = mapOf(":api" to ProjectMetadata("api", ":api", changedFiles = listOf("api/src/main/kotlin/Api.kt")))
        )

        // then — "api/" prefix is stripped because the project dir is resolved relative to rootDir
        result shouldNotContain "api/src/main/kotlin/Api.kt"
        result shouldContain "src/main/kotlin/Api.kt"
    }

    test("lists transitively affected project with via annotation") {
        // given
        val rootProject = ProjectBuilder.builder().build()
        ProjectBuilder.builder().withParent(rootProject).withName("api").build()
        ProjectBuilder.builder().withParent(rootProject).withName("app").build()

        val apiMetadata = ProjectMetadata(
            name = "api",
            fullyQualifiedName = ":api",
            changedFiles = listOf("api/src/main/kotlin/Api.kt")
        )
        val appMetadata = ProjectMetadata(
            name = "app",
            fullyQualifiedName = ":app",
            dependencies = listOf(apiMetadata)
        )
        val printer = ChangedProjectsPrinter(rootProject)

        // when
        val result = printer.buildReport(
            header = "Changed projects:",
            allAffectedProjects = setOf(":api", ":app"),
            changedFilesMap = mapOf(":api" to listOf("api/src/main/kotlin/Api.kt")),
            metadataMap = mapOf(":api" to apiMetadata, ":app" to appMetadata)
        )

        // then
        result shouldContain ":api"
        result shouldContain ":app"
        result shouldContain "affected via :api"
    }

    test("truncates file list when exceeding FILE_DISPLAY_LIMIT") {
        // given
        val rootProject = ProjectBuilder.builder().build()
        ProjectBuilder.builder().withParent(rootProject).withName("api").build()
        val manyFiles = (1..ChangedProjectsPrinter.FILE_DISPLAY_LIMIT + 5)
            .map { "api/src/main/kotlin/File$it.kt" }
        val printer = ChangedProjectsPrinter(rootProject)

        // when
        val result = printer.buildReport(
            header = "Changed projects:",
            allAffectedProjects = setOf(":api"),
            changedFilesMap = mapOf(":api" to manyFiles),
            metadataMap = mapOf(":api" to ProjectMetadata("api", ":api", changedFiles = manyFiles))
        )

        // then
        result shouldContain "... and 5 more"
    }

    test("multiple directly changed projects are listed sorted") {
        // given
        val rootProject = ProjectBuilder.builder().build()
        ProjectBuilder.builder().withParent(rootProject).withName("beta").build()
        ProjectBuilder.builder().withParent(rootProject).withName("alpha").build()
        val printer = ChangedProjectsPrinter(rootProject)

        // when
        val result = printer.buildReport(
            header = "Changed projects:",
            allAffectedProjects = setOf(":alpha", ":beta"),
            changedFilesMap = mapOf(
                ":beta" to listOf("beta/src/main/kotlin/Beta.kt"),
                ":alpha" to listOf("alpha/src/main/kotlin/Alpha.kt")
            ),
            metadataMap = mapOf(
                ":alpha" to ProjectMetadata("alpha", ":alpha", changedFiles = listOf("alpha/src/main/kotlin/Alpha.kt")),
                ":beta" to ProjectMetadata("beta", ":beta", changedFiles = listOf("beta/src/main/kotlin/Beta.kt"))
            )
        )

        // then
        val alphaIndex = result.indexOf(":alpha")
        val betaIndex = result.indexOf(":beta")
        (alphaIndex < betaIndex) shouldBe true
    }
})
