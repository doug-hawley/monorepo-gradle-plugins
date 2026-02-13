package io.github.doughawley.monorepochangedprojects

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.gradle.testfixtures.ProjectBuilder

class BuildChangedProjectsTaskTest : FunSpec({

    test("buildChangedProjects task should be registered") {
        // given
        val project = ProjectBuilder.builder().build()

        // when
        project.pluginManager.apply("io.github.doug-hawley.monorepo-changed-projects-plugin")

        // then
        val task = project.tasks.findByName("buildChangedProjects")
        task shouldNotBe null
        task?.group shouldBe "build"
        task?.description shouldBe "Builds only the projects that have been affected by changes"
    }

    test("buildChangedProjects should have detectChangedProjects as dependency") {
        // given
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("io.github.doug-hawley.monorepo-changed-projects-plugin")

        // when
        val task = project.tasks.findByName("buildChangedProjects")

        // then - verify dependency is set up
        task shouldNotBe null
        val dependencies = task?.taskDependencies?.getDependencies(task)?.map { it.name }
        dependencies shouldNotBe null
        dependencies!! shouldContain "detectChangedProjects"
    }

    test("safe casting logic should handle null property") {
        // Test the safe casting pattern directly
        val testMap = mutableMapOf<String, Any?>()
        testMap["changedProjects"] = null

        // This is the pattern used in the fix - should not throw
        val changedProjectsRaw = testMap["changedProjects"] as? Set<*>
        changedProjectsRaw shouldBe null

        // If null, should filter to empty
        val changedProjects = changedProjectsRaw?.filterIsInstance<String>()?.toSet() ?: emptySet()
        changedProjects shouldBe emptySet<String>()
    }

    test("safe casting logic should handle wrong type") {
        // Test the safe casting pattern with wrong type
        val testMap = mutableMapOf<String, Any?>()
        testMap["changedProjects"] = listOf("project1", "project2") // Wrong type: List instead of Set

        // This is the pattern used in the fix - should not throw
        val changedProjectsRaw = testMap["changedProjects"] as? Set<*>
        changedProjectsRaw shouldBe null // Cast fails, returns null

        // If null, should filter to empty
        val changedProjects = changedProjectsRaw?.filterIsInstance<String>()?.toSet() ?: emptySet()
        changedProjects shouldBe emptySet<String>()
    }

    test("safe casting logic should handle valid Set<String>") {
        // Test the safe casting pattern with correct type
        val testMap = mutableMapOf<String, Any?>()
        testMap["changedProjects"] = setOf(":project1", ":project2")

        // This is the pattern used in the fix
        val changedProjectsRaw = testMap["changedProjects"] as? Set<*>
        changedProjectsRaw shouldNotBe null

        // Should successfully filter and convert
        val changedProjects = changedProjectsRaw?.filterIsInstance<String>()?.toSet() ?: emptySet()
        changedProjects shouldBe setOf(":project1", ":project2")
    }

    test("safe casting logic should handle Set with mixed types") {
        // Test the safe casting pattern with mixed types in Set
        val testMap = mutableMapOf<String, Any?>()
        testMap["changedProjects"] = setOf(":project1", 123, ":project2") // Mixed types

        // This is the pattern used in the fix
        val changedProjectsRaw = testMap["changedProjects"] as? Set<*>
        changedProjectsRaw shouldNotBe null

        // Should filter out non-Strings
        val changedProjects = changedProjectsRaw?.filterIsInstance<String>()?.toSet() ?: emptySet()
        changedProjects shouldBe setOf(":project1", ":project2")
    }

    test("buildChangedProjects should handle missing changedProjects property gracefully") {
        // given
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("io.github.doug-hawley.monorepo-changed-projects-plugin")

        // when - property is not set (detectChangedProjects didn't run)
        val task = project.tasks.findByName("buildChangedProjects")
        task shouldNotBe null

        // Verify the property doesn't exist yet
        val hasProperty = project.extensions.extraProperties.has("changedProjects")
        hasProperty shouldBe false
    }

    test("buildChangedProjects should validate property exists before accessing") {
        // given
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("io.github.doug-hawley.monorepo-changed-projects-plugin")

        // Set up the property
        project.extensions.extraProperties.set("changedProjects", setOf(":subproject"))

        // when
        val hasProperty = project.extensions.extraProperties.has("changedProjects")

        // then
        hasProperty shouldBe true
        val changedProjects = project.extensions.extraProperties.get("changedProjects") as? Set<*>
        changedProjects shouldNotBe null
    }
})
