package io.github.doughawley.monorepochangedprojects.functional

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.TaskOutcome

/**
 * Functional tests verifying that the plugin correctly handles deeply nested project structures
 * (e.g., :services:billing:api — three or more levels of nesting).
 *
 * Project structure under test:
 *   :services:billing:api          (leaf — no project dependencies)
 *   :services:billing:impl         (depends on :services:billing:api)
 *   :services:payments:gateway     (depends on :services:billing:api)
 *   :apps:web                      (depends on :services:payments:gateway)
 *
 * Dependency chain: api → impl (dead end)
 *                   api → gateway → web
 */
class NestedProjectsFunctionalTest : FunSpec({
    val testProjectListener = listener(TestProjectListener())

    fun createNestedProject() = TestProjectBuilder(testProjectListener.getTestProjectDir())
        .withSubproject("services/billing/api")
        .withSubproject("services/billing/impl", dependsOn = listOf("services/billing/api"))
        .withSubproject("services/payments/gateway", dependsOn = listOf("services/billing/api"))
        .withSubproject("apps/web", dependsOn = listOf("services/payments/gateway"))
        .applyPlugin()
        .withRemote()
        .build()
        .also { project ->
            project.initGit()
            project.commitAll("Initial commit")
            project.pushToRemote()
        }

    test("plugin detects change in three-level-deep project") {
        val project = createNestedProject()

        project.appendToFile(
            "services/billing/api/src/main/kotlin/com/example/Api.kt",
            "\n// Modified"
        )
        project.commitAll("Change billing api")

        val result = project.runTask("detectChangedProjects")

        result.task(":detectChangedProjects")?.outcome shouldBe TaskOutcome.SUCCESS
        val changed = result.extractChangedProjects()
        changed shouldContain ":services:billing:api"
    }

    test("change in deeply nested project propagates to all transitive dependents") {
        val project = createNestedProject()

        project.appendToFile(
            "services/billing/api/src/main/kotlin/com/example/Api.kt",
            "\n// Modified"
        )
        project.commitAll("Change billing api")

        val result = project.runTask("detectChangedProjects")

        result.task(":detectChangedProjects")?.outcome shouldBe TaskOutcome.SUCCESS
        val changed = result.extractChangedProjects()
        changed shouldContainAll setOf(
            ":services:billing:api",
            ":services:billing:impl",
            ":services:payments:gateway",
            ":apps:web"
        )
    }

    test("change in mid-level nested project does not affect unrelated sibling branch") {
        val project = createNestedProject()

        // Modify gateway — should affect web, but NOT impl (impl depends only on api)
        project.appendToFile(
            "services/payments/gateway/src/main/kotlin/com/example/Gateway.kt",
            "\n// Modified"
        )
        project.commitAll("Change payments gateway")

        val result = project.runTask("detectChangedProjects")

        result.task(":detectChangedProjects")?.outcome shouldBe TaskOutcome.SUCCESS
        val changed = result.extractChangedProjects()
        changed shouldContainAll setOf(":services:payments:gateway", ":apps:web")
        changed shouldNotContain ":services:billing:api"
        changed shouldNotContain ":services:billing:impl"
    }

    test("change in deepest leaf project with no dependents flags only that project") {
        val project = createNestedProject()

        // impl depends on api but nothing depends on impl
        project.appendToFile(
            "services/billing/impl/src/main/kotlin/com/example/Impl.kt",
            "\n// Modified"
        )
        project.commitAll("Change billing impl")

        val result = project.runTask("detectChangedProjects")

        result.task(":detectChangedProjects")?.outcome shouldBe TaskOutcome.SUCCESS
        val changed = result.extractChangedProjects()
        changed shouldBe setOf(":services:billing:impl")
    }

    test("file-to-project mapping is correct for three-level-deep paths") {
        val project = createNestedProject()

        // Directly verify the project path reported is the full three-segment path
        project.appendToFile(
            "services/payments/gateway/src/main/kotlin/com/example/Gateway.kt",
            "\n// Modified"
        )
        project.commitAll("Change gateway")

        val result = project.runTask("detectChangedProjects")

        val changed = result.extractChangedProjects()
        // Full Gradle path must be :services:payments:gateway, not just :gateway or :payments:gateway
        changed shouldContain ":services:payments:gateway"
        changed shouldNotContain ":gateway"
        changed shouldNotContain ":payments:gateway"
    }
})
