package io.github.doughawley.monorepobuild.functional

import io.github.doughawley.monorepobuild.functional.StandardTestProject.Files
import io.github.doughawley.monorepobuild.functional.StandardTestProject.Projects
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.TaskOutcome

/**
 * Functional tests for the Monorepo Changed Projects plugin.
 * These tests create real Gradle projects, make git changes, and verify the plugin detects them correctly.
 */
class MonorepoPluginFunctionalTest : FunSpec({
    val testProjectListener = listener(TestProjectListener())

    test("plugin detects changed library and all dependent projects") {
        // Setup: Standard project structure
        val project = testProjectListener.createStandardProject()

        // Make changes to common-lib and commit them
        project.appendToFile(Files.COMMON_LIB_SOURCE, "\n// Added comment")
        project.commitAll("Change common-lib")

        // Execute
        val result = project.runTask("printChangedProjectsFromBranch")

        // Assert
        result.task(":printChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS

        val changedProjects = result.extractChangedProjects()
        changedProjects shouldHaveSize 5  // common-lib + all dependents
        changedProjects shouldContainAll setOf(
            Projects.COMMON_LIB,
            Projects.MODULE1,
            Projects.MODULE2,
            Projects.APP1,
            Projects.APP2
        )

        val directlyChanged = result.extractDirectlyChangedProjects()
        directlyChanged shouldHaveSize 1
        directlyChanged shouldContainAll setOf(Projects.COMMON_LIB)
    }

    test("plugin detects changed module and only its dependent apps") {
        // Setup
        val project = testProjectListener.createStandardProject()

        // Make changes only to module1 and commit
        project.appendToFile(Files.MODULE1_SOURCE, "\n// Modified module1")
        project.commitAll("Change module1")

        // Execute
        val result = project.runTask("printChangedProjectsFromBranch")

        // Assert
        result.task(":printChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS

        val changedProjects = result.extractChangedProjects()
        changedProjects shouldHaveSize 2  // module1 and app1 (not app2)
        changedProjects shouldContainAll setOf(Projects.MODULE1, Projects.APP1)
    }

    test("plugin detects changed module2 affecting both apps") {
        // Setup
        val project = testProjectListener.createStandardProject()

        // Make changes to module2 (which both apps depend on) and commit
        project.appendToFile(Files.MODULE2_SOURCE, "\n// Modified module2")
        project.commitAll("Change module2")

        // Execute
        val result = project.runTask("printChangedProjectsFromBranch")

        // Assert
        result.task(":printChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS

        val changedProjects = result.extractChangedProjects()
        changedProjects shouldHaveSize 3  // module2, app1, app2
        changedProjects shouldContainAll setOf(Projects.MODULE2, Projects.APP1, Projects.APP2)
    }

    test("plugin detects only leaf project when changed") {
        // Setup
        val project = testProjectListener.createStandardProject()

        // Make changes only to app1 (leaf project) and commit
        project.appendToFile(Files.APP1_SOURCE, "\n// Modified app")
        project.commitAll("Change app1")

        // Execute
        val result = project.runTask("printChangedProjectsFromBranch")

        // Assert
        result.task(":printChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS

        val changedProjects = result.extractChangedProjects()
        changedProjects shouldHaveSize 1
        changedProjects shouldContainAll setOf(Projects.APP1)
    }

    test("plugin detects no changes when nothing modified") {
        // Setup
        val project = testProjectListener.createStandardProject()

        // Don't make any changes

        // Execute
        val result = project.runTask("printChangedProjectsFromBranch")

        // Assert
        result.task(":printChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS

        result.extractDirectlyChangedProjects() shouldBe emptySet()
        result.extractChangedProjects() shouldBe emptySet()
    }

    test("plugin detects multiple independent app changes") {
        // Setup
        val project = testProjectListener.createStandardProject()

        // Make changes to both apps (independent changes) and commit
        project.appendToFile(Files.APP1_SOURCE, "\n// Modified app1")
        project.appendToFile(Files.APP2_SOURCE, "\n// Modified app2")
        project.commitAll("Change both apps")

        // Execute
        val result = project.runTask("printChangedProjectsFromBranch")

        // Assert
        result.task(":printChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS

        val changedProjects = result.extractChangedProjects()
        changedProjects shouldHaveSize 2
        changedProjects shouldContainAll setOf(Projects.APP1, Projects.APP2)
    }

    test("plugin detects untracked files when includeUntracked is true") {
        // Setup
        val project = testProjectListener.createStandardProject()

        // Create a new untracked file in common-lib (this tests untracked detection)
        project.createNewFile("common-lib/src/main/kotlin/com/example/NewFile.kt",
            """
            package com.example

            class NewFile
            """.trimIndent()
        )

        // Execute
        val result = project.runTask("printChangedProjectsFromBranch")

        // Assert
        result.task(":printChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS

        val changedProjects = result.extractChangedProjects()
        // common-lib changed, so all dependents affected
        changedProjects shouldContainAll setOf(
            Projects.COMMON_LIB,
            Projects.MODULE1,
            Projects.MODULE2,
            Projects.APP1,
            Projects.APP2
        )
    }

    test("plugin detects staged changes without committing") {
        // Setup
        val project = testProjectListener.createStandardProject()

        // Make and stage changes to module1 (but don't commit)
        project.appendToFile(Files.MODULE1_SOURCE, "\n// Staged change")
        project.stageFile(Files.MODULE1_SOURCE)

        // Execute
        val result = project.runTask("printChangedProjectsFromBranch")

        // Assert
        result.task(":printChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS

        val changedProjects = result.extractChangedProjects()
        changedProjects shouldContainAll setOf(Projects.MODULE1, Projects.APP1)
    }

    test("plugin works with build.gradle.kts changes") {
        // Setup
        val project = testProjectListener.createStandardProject()

        // Modify build file and commit
        project.appendToFile(Files.MODULE2_BUILD, "\n// Build config change")
        project.commitAll("Change build config")

        // Execute
        val result = project.runTask("printChangedProjectsFromBranch")

        // Assert
        result.task(":printChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS

        val changedProjects = result.extractChangedProjects()
        changedProjects shouldContainAll setOf(Projects.MODULE2, Projects.APP1, Projects.APP2)
    }

    test("plugin detects changes with :apps prefix") {
        // Setup
        val project = testProjectListener.createStandardProject()

        // Make changes to module2 (affects both apps) and commit
        project.appendToFile(Files.MODULE2_SOURCE, "\n// Changed")
        project.commitAll("Change module2")

        // Execute
        val result = project.runTask("printChangedProjectsFromBranch")

        // Assert
        val changedProjects = result.extractChangedProjects()

        // Filter for apps prefix - both apps should be affected
        val changedApps = changedProjects.filter { it.startsWith(":apps") }
        changedApps shouldHaveSize 2
        changedApps shouldContainAll setOf(Projects.APP1, Projects.APP2)
    }

    // -- Deeply nested project tests (3+ levels, e.g. :services:billing:api) --

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

        val result = project.runTask("printChangedProjectsFromBranch")

        result.task(":printChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS
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

        val result = project.runTask("printChangedProjectsFromBranch")

        result.task(":printChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS
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

        project.appendToFile(
            "services/payments/gateway/src/main/kotlin/com/example/Gateway.kt",
            "\n// Modified"
        )
        project.commitAll("Change payments gateway")

        val result = project.runTask("printChangedProjectsFromBranch")

        result.task(":printChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS
        val changed = result.extractChangedProjects()
        changed shouldContainAll setOf(":services:payments:gateway", ":apps:web")
        changed shouldNotContain ":services:billing:api"
        changed shouldNotContain ":services:billing:impl"
    }

    test("change in deepest leaf project with no dependents flags only that project") {
        val project = createNestedProject()

        project.appendToFile(
            "services/billing/impl/src/main/kotlin/com/example/Impl.kt",
            "\n// Modified"
        )
        project.commitAll("Change billing impl")

        val result = project.runTask("printChangedProjectsFromBranch")

        result.task(":printChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS
        val changed = result.extractChangedProjects()
        changed shouldBe setOf(":services:billing:impl")
    }

    test("file-to-project mapping is correct for three-level-deep paths") {
        val project = createNestedProject()

        project.appendToFile(
            "services/payments/gateway/src/main/kotlin/com/example/Gateway.kt",
            "\n// Modified"
        )
        project.commitAll("Change gateway")

        val result = project.runTask("printChangedProjectsFromBranch")

        val changed = result.extractChangedProjects()
        changed shouldContain ":services:payments:gateway"
        changed shouldNotContain ":gateway"
        changed shouldNotContain ":payments:gateway"
    }

    test("plugin detects staged changes to multiple projects") {
        // Setup
        val project = testProjectListener.createStandardProject()

        // Make changes to common-lib and app2, stage but don't commit
        project.appendToFile(Files.COMMON_LIB_SOURCE, "\n// Staged common change")
        project.stageFile(Files.COMMON_LIB_SOURCE)
        project.appendToFile(Files.APP2_SOURCE, "\n// Staged app change")
        project.stageFile(Files.APP2_SOURCE)

        // Execute
        val result = project.runTask("printChangedProjectsFromBranch")

        // Assert
        result.task(":printChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS

        val changedProjects = result.extractChangedProjects()
        // common-lib affects all projects, app2 adds itself
        changedProjects shouldHaveSize 5
        changedProjects shouldContainAll setOf(
            Projects.COMMON_LIB,
            Projects.MODULE1,
            Projects.MODULE2,
            Projects.APP1,
            Projects.APP2
        )
    }

    // -- Hierarchy node tests (intermediate directories with no build file) --

    test("hierarchy node does not appear in directly changed projects when child file changes") {
        // Setup: standard project has apps/ and modules/ as hierarchy nodes without build files
        val project = testProjectListener.createStandardProject()

        // Make changes to apps/app1
        project.appendToFile(Files.APP1_SOURCE, "\n// Modified")
        project.commitAll("Change app1")

        // Execute
        val result = project.runTask("printChangedProjects")

        // Assert
        result.task(":printChangedProjects")?.outcome shouldBe TaskOutcome.SUCCESS
        val directlyChanged = result.extractDirectlyChangedProjects()
        directlyChanged shouldContain Projects.APP1
        directlyChanged shouldNotContain ":apps"
    }

    test("hierarchy node does not appear in all affected projects when child file changes") {
        // Setup
        val project = testProjectListener.createStandardProject()

        // Make changes to modules/module1 (apps/app1 depends on it transitively)
        project.appendToFile(Files.MODULE1_SOURCE, "\n// Modified")
        project.commitAll("Change module1")

        // Execute
        val result = project.runTask("printChangedProjects")

        // Assert: real projects appear, hierarchy nodes do not
        result.task(":printChangedProjects")?.outcome shouldBe TaskOutcome.SUCCESS
        val changed = result.extractChangedProjects()
        changed shouldContain Projects.MODULE1
        changed shouldContain Projects.APP1
        changed shouldNotContain ":modules"
        changed shouldNotContain ":apps"
    }

    test("multiple hierarchy nodes are excluded when children in sibling trees change") {
        // Setup
        val project = testProjectListener.createStandardProject()

        project.appendToFile(Files.APP2_SOURCE, "\n// Modified")
        project.appendToFile(Files.MODULE2_SOURCE, "\n// Modified")
        project.commitAll("Change app2 and module2")

        // Execute
        val result = project.runTask("printChangedProjects")

        // Assert
        result.task(":printChangedProjects")?.outcome shouldBe TaskOutcome.SUCCESS
        val directlyChanged = result.extractDirectlyChangedProjects()
        directlyChanged shouldContain Projects.APP2
        directlyChanged shouldContain Projects.MODULE2
        directlyChanged shouldNotContain ":apps"
        directlyChanged shouldNotContain ":modules"
    }

    test("untracked file in nested project does not cause hierarchy node to appear") {
        // Setup
        val project = testProjectListener.createStandardProject()

        // Create an untracked file in modules/module2 (not committed)
        project.createNewFile(
            "modules/module2/src/main/kotlin/com/example/NewFeature.kt",
            "package com.example\nclass NewFeature"
        )

        // Execute
        val result = project.runTask("printChangedProjects")

        // Assert: :modules:module2 detected via untracked file, :modules is not
        result.task(":printChangedProjects")?.outcome shouldBe TaskOutcome.SUCCESS
        val directlyChanged = result.extractDirectlyChangedProjects()
        directlyChanged shouldContain Projects.MODULE2
        directlyChanged shouldNotContain ":modules"
    }

    test("three-level hierarchy nodes do not appear for deeply nested project change") {
        // Setup: project with three levels — :services:billing:api, :services:billing:impl
        val project = TestProjectBuilder(testProjectListener.getTestProjectDir())
            .withSubproject("services/billing/api")
            .withSubproject("services/billing/impl", dependsOn = listOf("services/billing/api"))
            .applyPlugin()
            .withRemote()
            .build()
        project.initGit()
        project.commitAll("Initial commit")
        project.pushToRemote()

        project.appendToFile(
            "services/billing/impl/src/main/kotlin/com/example/Impl.kt",
            "\n// Modified"
        )
        project.commitAll("Change billing impl")

        // Execute
        val result = project.runTask("printChangedProjects")

        // Assert: only the concrete project appears, not :services or :services:billing
        result.task(":printChangedProjects")?.outcome shouldBe TaskOutcome.SUCCESS
        val directlyChanged = result.extractDirectlyChangedProjects()
        directlyChanged shouldContain ":services:billing:impl"
        directlyChanged shouldNotContain ":services"
        directlyChanged shouldNotContain ":services:billing"
    }
})
