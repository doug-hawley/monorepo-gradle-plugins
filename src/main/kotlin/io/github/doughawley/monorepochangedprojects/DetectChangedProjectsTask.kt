package io.github.doughawley.monorepochangedprojects

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Task that detects which projects have changed based on git history and dependency analysis.
 */
abstract class DetectChangedProjectsTask : DefaultTask() {

    private val gitDetector by lazy { GitChangedFilesDetector(logger) }
    private val projectMapper by lazy { ProjectFileMapper() }
    private val metadataFactory by lazy { ProjectMetadataFactory(logger) }

    @TaskAction
    fun detectChanges() {
        val extension = project.extensions.getByType(ProjectsChangedExtension::class.java)

        logger.lifecycle("Detecting changed projects...")
        logger.lifecycle("Base branch: ${extension.baseBranch}")
        logger.lifecycle("Include untracked: ${extension.includeUntracked}")

        val changedFiles = gitDetector.getChangedFiles(project.rootDir, extension)
        val changedFilesMap = projectMapper.mapChangedFilesToProjects(project.rootProject, changedFiles)
        val directlyChangedProjects = changedFilesMap.keys

        // Build metadata with changed files information
        val metadataMap = metadataFactory.buildProjectMetadataMap(project.rootProject, changedFilesMap)

        // Get all affected projects (those with changes OR dependency changes) using recursive hasChanges()
        // Exclude the root project and container projects (projects without build files)
        val allAffectedProjects = metadataMap.values
            .filter { metadata ->
                metadata.hasChanges() &&
                metadata.fullyQualifiedName != ":" &&
                hasBuildFile(project.rootProject, metadata.fullyQualifiedName)
            }
            .map { it.fullyQualifiedName }
            .toSet()

        logger.lifecycle("Changed files count: ${changedFiles.size}")

        val directlyChangedList = if (directlyChangedProjects.isEmpty()) "" else directlyChangedProjects.joinToString(", ")
        logger.lifecycle("Directly changed projects: $directlyChangedList")

        val allAffectedList = if (allAffectedProjects.isEmpty()) "" else allAffectedProjects.joinToString(", ")
        logger.lifecycle("All affected projects (including dependents): $allAffectedList")

        if (allAffectedProjects.isEmpty()) {
            logger.lifecycle("No projects have changed")
        }

        // Store results in project extra properties for other tasks to use
        project.extensions.extraProperties.set("changedProjects", allAffectedProjects)
        project.extensions.extraProperties.set("changedProjectsMetadata", metadataMap)
        project.extensions.extraProperties.set("changedFilesMap", changedFilesMap)
    }

    /**
     * Checks if a project has a build file (build.gradle or build.gradle.kts).
     * Projects without build files are just containers and shouldn't be built.
     */
    private fun hasBuildFile(rootProject: org.gradle.api.Project, projectPath: String): Boolean {
        val targetProject = rootProject.findProject(projectPath) ?: return false
        val projectDir = targetProject.projectDir
        return projectDir.resolve("build.gradle.kts").exists() ||
               projectDir.resolve("build.gradle").exists()
    }
}
