package io.github.doughawley.monorepobuild

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.gradle.testfixtures.ProjectBuilder

class BuildChangedProjectsTaskTest : FunSpec({

    test("buildChangedProjects task should be registered") {
        // given
        val project = ProjectBuilder.builder().build()

        // when
        project.pluginManager.apply("io.github.doug-hawley.monorepo-build-plugin")

        // then
        val task = project.tasks.findByName("buildChangedProjects")
        task shouldNotBe null
        task?.group shouldBe "build"
        task?.description shouldBe "Builds only the projects that have been affected by changes"
    }
})
