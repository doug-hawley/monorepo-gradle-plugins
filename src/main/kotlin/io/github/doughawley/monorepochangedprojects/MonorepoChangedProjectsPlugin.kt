package io.github.doughawley.monorepochangedprojects

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

        // Register the detectChangedProjects task
        project.tasks.register("detectChangedProjects", DetectChangedProjectsTask::class.java).configure {
            group = "verification"
            description = "Detects which projects have changed based on git history"
        }

        // Register the buildChangedProjects task
        project.tasks.register("buildChangedProjects").configure {
            group = "build"
            description = "Builds only the projects that have been affected by changes"
            dependsOn("detectChangedProjects")

            doLast {
                val changedProjects = project.extensions.extraProperties.get("changedProjects") as Set<String>

                if (changedProjects.isEmpty()) {
                    project.logger.lifecycle("No projects have changed - nothing to build")
                } else {
                    project.logger.lifecycle("Building ${changedProjects.size} changed project(s): ${changedProjects.joinToString(", ")}")

                    changedProjects.forEach { projectPath ->
                        val targetProject = project.findProject(projectPath)
                        if (targetProject != null) {
                            project.logger.lifecycle("Building $projectPath...")

                            // Find and execute the build task for this project
                            val buildTask = targetProject.tasks.findByName("build")
                            if (buildTask != null) {
                                buildTask.actions.forEach { action ->
                                    action.execute(buildTask)
                                }
                            } else {
                                project.logger.warn("No build task found for $projectPath")
                            }
                        } else {
                            project.logger.warn("Project not found: $projectPath")
                        }
                    }

                    project.logger.lifecycle("Successfully built ${changedProjects.size} changed project(s)")
                }
            }
        }

        project.logger.lifecycle("Projects Changed Plugin applied to ${project.name}")
    }
}
