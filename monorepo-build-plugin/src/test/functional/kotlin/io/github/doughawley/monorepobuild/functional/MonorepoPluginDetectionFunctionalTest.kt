package io.github.doughawley.monorepobuild.functional

import io.github.doughawley.monorepobuild.functional.StandardTestProject.Files
import io.github.doughawley.monorepobuild.functional.StandardTestProject.Projects
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.TaskOutcome

/**
 * Functional tests for basic changed-project detection: committed, staged, and untracked changes.
 */
class MonorepoPluginDetectionFunctionalTest : FunSpec({
    val testProjectListener = listener(TestProjectListener())

    test("plugin detects changed library and all dependent projects") {
        // given
        val project = testProjectListener.createStandardProject()

        // when
        project.appendToFile(Files.COMMON_LIB_SOURCE, "\n// Added comment")
        project.commitAll("Change common-lib")
        val result = project.runTask("printChangedProjectsFromBranch")

        // then
        result.task(":printChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS
        val changedProjects = result.extractChangedProjects()
        changedProjects shouldHaveSize 5
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
        // given
        val project = testProjectListener.createStandardProject()

        // when
        project.appendToFile(Files.MODULE1_SOURCE, "\n// Modified module1")
        project.commitAll("Change module1")
        val result = project.runTask("printChangedProjectsFromBranch")

        // then
        result.task(":printChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS
        val changedProjects = result.extractChangedProjects()
        changedProjects shouldHaveSize 2
        changedProjects shouldContainAll setOf(Projects.MODULE1, Projects.APP1)
    }

    test("plugin detects changed module2 affecting both apps") {
        // given
        val project = testProjectListener.createStandardProject()

        // when
        project.appendToFile(Files.MODULE2_SOURCE, "\n// Modified module2")
        project.commitAll("Change module2")
        val result = project.runTask("printChangedProjectsFromBranch")

        // then
        result.task(":printChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS
        val changedProjects = result.extractChangedProjects()
        changedProjects shouldHaveSize 3
        changedProjects shouldContainAll setOf(Projects.MODULE2, Projects.APP1, Projects.APP2)
    }

    test("plugin detects only leaf project when changed") {
        // given
        val project = testProjectListener.createStandardProject()

        // when
        project.appendToFile(Files.APP1_SOURCE, "\n// Modified app")
        project.commitAll("Change app1")
        val result = project.runTask("printChangedProjectsFromBranch")

        // then
        result.task(":printChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS
        val changedProjects = result.extractChangedProjects()
        changedProjects shouldHaveSize 1
        changedProjects shouldContainAll setOf(Projects.APP1)
    }

    test("plugin detects no changes when nothing modified") {
        // given
        val project = testProjectListener.createStandardProject()

        // when
        val result = project.runTask("printChangedProjectsFromBranch")

        // then
        result.task(":printChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS
        result.extractDirectlyChangedProjects() shouldBe emptySet()
        result.extractChangedProjects() shouldBe emptySet()
    }

    test("plugin detects multiple independent app changes") {
        // given
        val project = testProjectListener.createStandardProject()

        // when
        project.appendToFile(Files.APP1_SOURCE, "\n// Modified app1")
        project.appendToFile(Files.APP2_SOURCE, "\n// Modified app2")
        project.commitAll("Change both apps")
        val result = project.runTask("printChangedProjectsFromBranch")

        // then
        result.task(":printChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS
        val changedProjects = result.extractChangedProjects()
        changedProjects shouldHaveSize 2
        changedProjects shouldContainAll setOf(Projects.APP1, Projects.APP2)
    }

    test("plugin detects untracked files when includeUntracked is true") {
        // given
        val project = testProjectListener.createStandardProject()

        // when
        project.createNewFile(
            "common-lib/src/main/kotlin/com/example/NewFile.kt",
            """
            package com.example

            class NewFile
            """.trimIndent()
        )
        val result = project.runTask("printChangedProjectsFromBranch")

        // then
        result.task(":printChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS
        val changedProjects = result.extractChangedProjects()
        changedProjects shouldContainAll setOf(
            Projects.COMMON_LIB,
            Projects.MODULE1,
            Projects.MODULE2,
            Projects.APP1,
            Projects.APP2
        )
    }

    test("plugin detects staged changes without committing") {
        // given
        val project = testProjectListener.createStandardProject()

        // when
        project.appendToFile(Files.MODULE1_SOURCE, "\n// Staged change")
        project.stageFile(Files.MODULE1_SOURCE)
        val result = project.runTask("printChangedProjectsFromBranch")

        // then
        result.task(":printChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS
        val changedProjects = result.extractChangedProjects()
        changedProjects shouldContainAll setOf(Projects.MODULE1, Projects.APP1)
    }

    test("plugin works with build.gradle.kts changes") {
        // given
        val project = testProjectListener.createStandardProject()

        // when
        project.appendToFile(Files.MODULE2_BUILD, "\n// Build config change")
        project.commitAll("Change build config")
        val result = project.runTask("printChangedProjectsFromBranch")

        // then
        result.task(":printChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS
        val changedProjects = result.extractChangedProjects()
        changedProjects shouldContainAll setOf(Projects.MODULE2, Projects.APP1, Projects.APP2)
    }

    test("plugin detects changes with :apps prefix") {
        // given
        val project = testProjectListener.createStandardProject()

        // when
        project.appendToFile(Files.MODULE2_SOURCE, "\n// Changed")
        project.commitAll("Change module2")
        val result = project.runTask("printChangedProjectsFromBranch")

        // then
        val changedProjects = result.extractChangedProjects()
        val changedApps = changedProjects.filter { it.startsWith(":apps") }
        changedApps shouldHaveSize 2
        changedApps shouldContainAll setOf(Projects.APP1, Projects.APP2)
    }

    test("plugin detects staged changes to multiple projects") {
        // given
        val project = testProjectListener.createStandardProject()

        // when
        project.appendToFile(Files.COMMON_LIB_SOURCE, "\n// Staged common change")
        project.stageFile(Files.COMMON_LIB_SOURCE)
        project.appendToFile(Files.APP2_SOURCE, "\n// Staged app change")
        project.stageFile(Files.APP2_SOURCE)
        val result = project.runTask("printChangedProjectsFromBranch")

        // then
        result.task(":printChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS
        val changedProjects = result.extractChangedProjects()
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
