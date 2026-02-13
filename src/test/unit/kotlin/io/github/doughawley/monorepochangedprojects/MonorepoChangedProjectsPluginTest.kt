package io.github.doughawley.monorepochangedprojects

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.gradle.testfixtures.ProjectBuilder

class MonorepoChangedProjectsPluginTest : FunSpec({

    test("plugin registers task") {
        // given
        val project = ProjectBuilder.builder().build()

        // when
        project.pluginManager.apply("io.github.doug-hawley.monorepo-changed-projects-plugin")

        // then
        val task = project.tasks.findByName("detectChangedProjects")
        task shouldNotBe null
        task.shouldBeInstanceOf<DetectChangedProjectsTask>()
    }

    test("plugin registers extension") {
        // given
        val project = ProjectBuilder.builder().build()

        // when
        project.pluginManager.apply("io.github.doug-hawley.monorepo-changed-projects-plugin")

        // then
        val extension = project.extensions.findByName("projectsChanged")
        extension shouldNotBe null
        extension.shouldBeInstanceOf<ProjectsChangedExtension>()
    }

    test("extension has correct defaults") {
        // given
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("io.github.doug-hawley.monorepo-changed-projects-plugin")

        // when
        val extension = project.extensions.getByType(ProjectsChangedExtension::class.java)

        // then
        extension.baseBranch shouldBe "main"
        extension.includeUntracked shouldBe true
        extension.excludePatterns shouldBe emptyList()
    }

    test("extension can be configured") {
        // given
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("io.github.doug-hawley.monorepo-changed-projects-plugin")
        val extension = project.extensions.getByType(ProjectsChangedExtension::class.java)

        // when
        extension.baseBranch = "develop"
        extension.includeUntracked = false
        extension.excludePatterns = listOf(".*\\.md", "docs/.*")

        // then
        extension.baseBranch shouldBe "develop"
        extension.includeUntracked shouldBe false
        extension.excludePatterns.size shouldBe 2
    }

    test("task has correct group and description") {
        // given
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("io.github.doug-hawley.monorepo-changed-projects-plugin")

        // when
        val task = project.tasks.findByName("detectChangedProjects")

        // then
        task shouldNotBe null
        task!!.group shouldBe "verification"
        task.description shouldBe "Detects which projects have changed based on git history"
    }

    test("plugin can detect projects with no git repository") {
        // given
        val tempDir = kotlin.io.path.createTempDirectory("test-no-git").toFile()
        try {
            val project = ProjectBuilder.builder()
                .withProjectDir(tempDir)
                .build()
            project.pluginManager.apply("io.github.doug-hawley.monorepo-changed-projects-plugin")
            val task = project.tasks.findByName("detectChangedProjects") as DetectChangedProjectsTask

            // when
            task.detectChanges()

            // then
            val changedProjects = project.extensions.extraProperties.get("changedProjects") as Set<String>
            changedProjects shouldBe emptySet()
        } finally {
            tempDir.deleteRecursively()
        }
    }
})
