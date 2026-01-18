package com.bitmoxie.monorepochangedprojects

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Gradle plugin that detects which projects have changed based on git history.
 */
class MonorepoChangedProjectsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Register the extension
        project.extensions.create(
            "projectsChanged",
            ProjectsChangedExtension::class.java
        )

        // Register the task
        project.tasks.register("detectChangedProjects", DetectChangedProjectsTask::class.java).configure {
            group = "verification"
            description = "Detects which projects have changed based on git history"
        }

        project.logger.lifecycle("Projects Changed Plugin applied to ${project.name}")
    }
}
