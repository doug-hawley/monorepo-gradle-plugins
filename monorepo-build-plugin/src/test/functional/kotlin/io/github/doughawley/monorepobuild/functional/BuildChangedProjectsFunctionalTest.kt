package io.github.doughawley.monorepobuild.functional

import io.github.doughawley.monorepobuild.functional.StandardTestProject.Files
import io.github.doughawley.monorepobuild.functional.StandardTestProject.Projects
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome

/**
 * Functional tests for the buildChangedProjectsFromBranch task.
 */
class BuildChangedProjectsFunctionalTest : FunSpec({
    val testProjectListener = listener(TestProjectListener())

    test("buildChangedProjectsFromBranch task builds only affected projects") {
        // Setup
        val project = testProjectListener.createStandardProject()

        // Make changes only to common-lib and commit
        project.appendToFile(Files.COMMON_LIB_SOURCE, "\n// Modified")
        project.commitAll("Change common-lib")

        // Execute
        val result = project.runTask("buildChangedProjectsFromBranch")

        // Assert
        result.task(":buildChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS

        // Should build common-lib and all its dependents
        val builtProjects = result.extractBuiltProjects()
        builtProjects shouldContainAll setOf(
            Projects.COMMON_LIB,
            Projects.MODULE1,
            Projects.MODULE2,
            Projects.APP1,
            Projects.APP2
        )
    }

    test("buildChangedProjectsFromBranch builds only affected apps when module changes") {
        // Setup
        val project = testProjectListener.createStandardProject()

        // Make changes only to module1 (only app1 depends on it) and commit
        project.appendToFile(Files.MODULE1_SOURCE, "\n// Modified")
        project.commitAll("Change module1")

        // Execute
        val result = project.runTask("buildChangedProjectsFromBranch")

        // Assert
        result.task(":buildChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS

        val builtProjects = result.extractBuiltProjects()
        builtProjects shouldContainAll setOf(Projects.MODULE1, Projects.APP1)
        builtProjects shouldNotContain Projects.APP2  // app2 doesn't depend on module1
    }

    test("buildChangedProjectsFromBranch reports no changes when nothing modified") {
        // Setup
        val project = testProjectListener.createStandardProject()

        // Don't make any changes

        // Execute
        val result = project.runTask("buildChangedProjectsFromBranch")

        // Assert
        result.task(":buildChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS
        result.output shouldContain "No projects have changed - nothing to build"
    }

    test("buildChangedProjectsFromBranch handles multiple independent app changes") {
        // Setup
        val project = testProjectListener.createStandardProject()

        // Make changes to both apps and commit
        project.appendToFile(Files.APP1_SOURCE, "\n// Modified A")
        project.appendToFile(Files.APP2_SOURCE, "\n// Modified B")
        project.commitAll("Change both apps")

        // Execute
        val result = project.runTask("buildChangedProjectsFromBranch")

        // Assert
        result.task(":buildChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS

        val builtProjects = result.extractBuiltProjects()
        builtProjects shouldContainAll setOf(Projects.APP1, Projects.APP2)
    }

    test("buildChangedProjectsFromBranch succeeds without running printChangedProjectsFromBranch") {
        // Setup
        val project = testProjectListener.createStandardProject()

        project.appendToFile(Files.MODULE2_SOURCE, "\n// Changed")
        project.commitAll("Change module2")

        // Execute
        val result = project.runTask("buildChangedProjectsFromBranch")

        // Assert - buildChangedProjectsFromBranch runs independently; printChangedProjectsFromBranch is not triggered
        result.task(":buildChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":printChangedProjectsFromBranch") shouldBe null
    }

    test("buildChangedProjectsFromBranch builds only leaf project when changed") {
        // Setup
        val project = testProjectListener.createStandardProject()

        // Make changes only to app2 and commit
        project.appendToFile(Files.APP2_SOURCE, "\n// App changed")
        project.commitAll("Change app2")

        // Execute
        val result = project.runTask("buildChangedProjectsFromBranch")

        // Assert
        result.task(":buildChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS

        val builtProjects = result.extractBuiltProjects()
        builtProjects shouldContain Projects.APP2
        builtProjects shouldNotContain Projects.COMMON_LIB
        builtProjects shouldNotContain Projects.MODULE1
        builtProjects shouldNotContain Projects.MODULE2
        builtProjects shouldNotContain Projects.APP1
    }

    test("buildChangedProjectsFromBranch builds projects affected by BOM changes") {
        // Setup
        val project = testProjectListener.createStandardProject()

        // Make changes to BOM and commit
        project.appendToFile(Files.PLATFORM_BUILD, "\n// BOM version bump")
        project.commitAll("Bump BOM version")

        // Execute
        val result = project.runTask("buildChangedProjectsFromBranch")

        // Assert
        result.task(":buildChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS

        // Should build all projects that depend on the BOM
        val builtProjects = result.extractBuiltProjects()
        builtProjects shouldContainAll setOf(
            Projects.PLATFORM,
            Projects.COMMON_LIB,
            Projects.MODULE1,
            Projects.MODULE2,
            Projects.APP1,
            Projects.APP2
        )
    }

    test("plugin detects BOM changes and marks all dependent projects as changed") {
        // Setup
        val project = testProjectListener.createStandardProject()

        // Make changes to BOM and commit
        project.appendToFile(Files.PLATFORM_BUILD, "\n// BOM version update")
        project.commitAll("Update BOM")

        // Execute
        val result = project.runTask("printChangedProjectsFromBranch")

        // Assert
        result.task(":printChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS

        val changedProjects = result.extractChangedProjects()
        // BOM changed, so all projects that depend on it should be affected
        changedProjects shouldHaveSize 6  // platform + all 5 dependents
        changedProjects shouldContainAll setOf(
            Projects.PLATFORM,
            Projects.COMMON_LIB,
            Projects.MODULE1,
            Projects.MODULE2,
            Projects.APP1,
            Projects.APP2
        )

        val directlyChanged = result.extractDirectlyChangedProjects()
        directlyChanged shouldHaveSize 1
        directlyChanged shouldContainAll setOf(Projects.PLATFORM)
    }

    test("plugin detects changes when both BOM and common-lib change") {
        // Setup
        val project = testProjectListener.createStandardProject()

        // Make changes to both BOM and common-lib, then commit
        project.appendToFile(Files.PLATFORM_BUILD, "\n// BOM update")
        project.appendToFile(Files.COMMON_LIB_SOURCE, "\n// Common-lib change")
        project.commitAll("Update BOM and common-lib")

        // Execute
        val result = project.runTask("printChangedProjectsFromBranch")

        // Assert
        result.task(":printChangedProjectsFromBranch")?.outcome shouldBe TaskOutcome.SUCCESS

        val changedProjects = result.extractChangedProjects()
        // All projects affected (BOM affects all, common-lib also changed)
        changedProjects shouldHaveSize 6
        changedProjects shouldContainAll setOf(
            Projects.PLATFORM,
            Projects.COMMON_LIB,
            Projects.MODULE1,
            Projects.MODULE2,
            Projects.APP1,
            Projects.APP2
        )
    }

})
