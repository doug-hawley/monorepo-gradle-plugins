package com.bitmoxie.monorepochangedprojects.functional

import com.bitmoxie.monorepochangedprojects.functional.StandardTestProject.Files
import com.bitmoxie.monorepochangedprojects.functional.StandardTestProject.Projects
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.TaskOutcome
import java.io.File

/**
 * Functional tests for the buildChangedProjects task.
 */
class BuildChangedProjectsFunctionalTest : FunSpec({
    lateinit var testProjectDir: File

    beforeEach {
        testProjectDir = kotlin.io.path.createTempDirectory("build-changed-test").toFile()
    }

    afterEach {
        testProjectDir.deleteRecursively()
    }

    test("buildChangedProjects task builds only affected projects") {
        // Setup
        val project = StandardTestProject.createAndInitialize(testProjectDir)

        // Make changes only to common-lib
        project.appendToFile(Files.COMMON_LIB_SOURCE, "\n// Modified")

        // Execute
        val result = project.runTask("buildChangedProjects")

        // Assert
        result.task(":buildChangedProjects")?.outcome shouldBe TaskOutcome.SUCCESS

        // Should build common-lib and all its dependents (5 projects total)
        result.output shouldContain "Building 5 changed project(s)"
        result.output shouldContain Projects.COMMON_LIB
        result.output shouldContain Projects.MODULE1
        result.output shouldContain Projects.MODULE2
        result.output shouldContain Projects.APP1
        result.output shouldContain Projects.APP2
    }

    test("buildChangedProjects builds only affected apps when module changes") {
        // Setup
        val project = StandardTestProject.createAndInitialize(testProjectDir)

        // Make changes only to module1 (only app1 depends on it)
        project.appendToFile(Files.MODULE1_SOURCE, "\n// Modified")

        // Execute
        val result = project.runTask("buildChangedProjects")

        // Assert
        result.task(":buildChangedProjects")?.outcome shouldBe TaskOutcome.SUCCESS

        result.output shouldContain "Building 2 changed project(s)"
        result.output shouldContain Projects.MODULE1
        result.output shouldContain Projects.APP1
        result.output shouldNotContain Projects.APP2  // app2 doesn't depend on module1
    }

    test("buildChangedProjects reports no changes when nothing modified") {
        // Setup
        val project = StandardTestProject.createAndInitialize(testProjectDir)

        // Don't make any changes

        // Execute
        val result = project.runTask("buildChangedProjects")

        // Assert
        result.task(":buildChangedProjects")?.outcome shouldBe TaskOutcome.SUCCESS
        result.output shouldContain "No projects have changed - nothing to build"
    }

    test("buildChangedProjects handles multiple independent app changes") {
        // Setup
        val project = StandardTestProject.createAndInitialize(testProjectDir)

        // Make changes to both apps
        project.appendToFile(Files.APP1_SOURCE, "\n// Modified A")
        project.appendToFile(Files.APP2_SOURCE, "\n// Modified B")

        // Execute
        val result = project.runTask("buildChangedProjects")

        // Assert
        result.task(":buildChangedProjects")?.outcome shouldBe TaskOutcome.SUCCESS
        result.output shouldContain "Building 2 changed project(s)"
        result.output shouldContain Projects.APP1
        result.output shouldContain Projects.APP2
    }

    test("buildChangedProjects runs after detectChangedProjects") {
        // Setup
        val project = StandardTestProject.createAndInitialize(testProjectDir)

        project.appendToFile(Files.MODULE2_SOURCE, "\n// Changed")

        // Execute
        val result = project.runTask("buildChangedProjects")

        // Assert - both tasks should have run
        result.task(":detectChangedProjects")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":buildChangedProjects")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    test("buildChangedProjects builds only leaf project when changed") {
        // Setup
        val project = StandardTestProject.createAndInitialize(testProjectDir)

        // Make changes only to app2
        project.appendToFile(Files.APP2_SOURCE, "\n// App changed")

        // Execute
        val result = project.runTask("buildChangedProjects")

        // Assert
        result.task(":buildChangedProjects")?.outcome shouldBe TaskOutcome.SUCCESS
        result.output shouldContain "Building 1 changed project(s)"
        result.output shouldContain Projects.APP2
        result.output shouldNotContain Projects.COMMON_LIB
        result.output shouldNotContain Projects.MODULE1
        result.output shouldNotContain Projects.MODULE2
        result.output shouldNotContain Projects.APP1
    }
})
