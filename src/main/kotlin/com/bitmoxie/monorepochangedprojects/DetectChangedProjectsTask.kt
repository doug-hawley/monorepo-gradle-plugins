package com.bitmoxie.monorepochangedprojects

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Task that detects which projects have changed based on git history and dependency analysis.
 */
abstract class DetectChangedProjectsTask : DefaultTask() {

    private val gitDetector by lazy { GitChangedFilesDetector(logger) }
    private val projectMapper by lazy { ProjectFileMapper() }
    private val dependencyAnalyzer by lazy { DependencyAnalyzer(logger) }

    @TaskAction
    fun detectChanges() {
        val extension = project.extensions.getByType(ProjectsChangedExtension::class.java)

        logger.lifecycle("Detecting changed projects...")
        logger.lifecycle("Base branch: ${extension.baseBranch}")
        logger.lifecycle("Include untracked: ${extension.includeUntracked}")

        val changedFiles = gitDetector.getChangedFiles(project.rootDir, extension)
        val directlyChangedProjects = projectMapper.findProjectsWithChangedFiles(project.rootProject, changedFiles)
        val allAffectedProjects = dependencyAnalyzer.findAllAffectedProjects(project.rootProject, directlyChangedProjects)

        logger.lifecycle("Changed files count: ${changedFiles.size}")
        logger.lifecycle("Directly changed projects: ${directlyChangedProjects.joinToString(", ")}")
        logger.lifecycle("All affected projects (including dependents): ${allAffectedProjects.joinToString(", ")}")

        // Store results in project extra properties for other tasks to use
        project.extensions.extraProperties.set("changedProjects", allAffectedProjects)
    }
}
