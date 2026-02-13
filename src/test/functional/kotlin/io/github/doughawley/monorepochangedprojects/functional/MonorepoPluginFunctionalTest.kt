package io.github.doughawley.monorepochangedprojects.functional

import io.github.doughawley.monorepochangedprojects.functional.StandardTestProject.Files
import io.github.doughawley.monorepochangedprojects.functional.StandardTestProject.Projects
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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
        val result = project.runTask("detectChangedProjects")

        // Assert
        result.task(":detectChangedProjects")?.outcome shouldBe TaskOutcome.SUCCESS

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
        val result = project.runTask("detectChangedProjects")

        // Assert
        result.task(":detectChangedProjects")?.outcome shouldBe TaskOutcome.SUCCESS

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
        val result = project.runTask("detectChangedProjects")

        // Assert
        result.task(":detectChangedProjects")?.outcome shouldBe TaskOutcome.SUCCESS

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
        val result = project.runTask("detectChangedProjects")

        // Assert
        result.task(":detectChangedProjects")?.outcome shouldBe TaskOutcome.SUCCESS

        val changedProjects = result.extractChangedProjects()
        changedProjects shouldHaveSize 1
        changedProjects shouldContainAll setOf(Projects.APP1)
    }

    test("plugin detects no changes when nothing modified") {
        // Setup
        val project = testProjectListener.createStandardProject()

        // Don't make any changes

        // Execute
        val result = project.runTask("detectChangedProjects")

        // Assert
        result.task(":detectChangedProjects")?.outcome shouldBe TaskOutcome.SUCCESS

        val changedFilesCount = result.extractChangedFilesCount()
        changedFilesCount shouldBe 0

        result.output shouldContain "No projects have changed"
    }

    test("plugin detects multiple independent app changes") {
        // Setup
        val project = testProjectListener.createStandardProject()

        // Make changes to both apps (independent changes) and commit
        project.appendToFile(Files.APP1_SOURCE, "\n// Modified app1")
        project.appendToFile(Files.APP2_SOURCE, "\n// Modified app2")
        project.commitAll("Change both apps")

        // Execute
        val result = project.runTask("detectChangedProjects")

        // Assert
        result.task(":detectChangedProjects")?.outcome shouldBe TaskOutcome.SUCCESS

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
        val result = project.runTask("detectChangedProjects")

        // Assert
        result.task(":detectChangedProjects")?.outcome shouldBe TaskOutcome.SUCCESS

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
        val result = project.runTask("detectChangedProjects")

        // Assert
        result.task(":detectChangedProjects")?.outcome shouldBe TaskOutcome.SUCCESS

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
        val result = project.runTask("detectChangedProjects")

        // Assert
        result.task(":detectChangedProjects")?.outcome shouldBe TaskOutcome.SUCCESS

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
        val result = project.runTask("detectChangedProjects")

        // Assert
        val changedProjects = result.extractChangedProjects()

        // Filter for apps prefix - both apps should be affected
        val changedApps = changedProjects.filter { it.startsWith(":apps") }
        changedApps shouldHaveSize 2
        changedApps shouldContainAll setOf(Projects.APP1, Projects.APP2)
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
        val result = project.runTask("detectChangedProjects")

        // Assert
        result.task(":detectChangedProjects")?.outcome shouldBe TaskOutcome.SUCCESS

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
})




