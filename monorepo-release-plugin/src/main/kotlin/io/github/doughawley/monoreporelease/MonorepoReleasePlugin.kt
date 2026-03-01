package io.github.doughawley.monoreporelease

import io.github.doughawley.monorepocore.git.GitCommandExecutor
import io.github.doughawley.monoreporelease.git.GitReleaseExecutor
import io.github.doughawley.monoreporelease.git.GitTagScanner
import io.github.doughawley.monoreporelease.task.ReleaseTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class MonorepoReleasePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val rootExtension = project.extensions.create("monorepoRelease", MonorepoReleaseExtension::class.java)

        // Register per-project opt-in extension and release tasks eagerly so that build scripts
        // can configure tasks like postRelease during their own configuration phase.
        project.subprojects.forEach { sub ->
            val config = sub.extensions.create("monorepoReleaseConfig", MonorepoReleaseConfigExtension::class.java)
            registerReleaseTasks(sub, rootExtension, config)
        }
    }

    private fun registerReleaseTasks(
        sub: Project,
        rootExtension: MonorepoReleaseExtension,
        config: MonorepoReleaseConfigExtension
    ) {
        val executor = GitCommandExecutor(sub.logger)
        val scanner = GitTagScanner(sub.rootProject.rootDir, executor)
        val releaseExecutor = GitReleaseExecutor(sub.rootProject.rootDir, executor, sub.logger)

        val postRelease = sub.tasks.register("postRelease") {
            group = "monorepo-release"
            description = "Lifecycle hook: wire publish tasks here via finalizedBy"
        }

        val releaseTask = sub.tasks.register("release", ReleaseTask::class.java) {
            group = "monorepo-release"
            description = "Creates a versioned git tag for this project"
            this.rootExtension = rootExtension
            this.projectConfig = config
            this.gitTagScanner = scanner
            this.gitReleaseExecutor = releaseExecutor
            finalizedBy(postRelease)
        }

        postRelease.configure {
            onlyIf {
                val state = releaseTask.get().state
                val failure: Throwable? = state.failure
                state.executed && failure == null
            }
        }
    }
}
