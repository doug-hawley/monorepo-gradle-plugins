package com.bitmoxie.monorepochangedprojects

import com.bitmoxie.monorepochangedprojects.domain.ProjectMetadata
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.logging.Logger

/**
 * Factory for building ProjectMetadata trees from Gradle Project objects.
 */
class ProjectMetadataFactory(private val logger: Logger) {

    /**
     * Builds a map of ProjectMetadata objects for all projects in the hierarchy.
     * Each ProjectMetadata includes its dependencies as a list of other ProjectMetadata objects.
     *
     * @param rootProject The root Gradle project
     * @return Map of project paths to ProjectMetadata objects
     */
    fun buildProjectMetadataMap(rootProject: Project): Map<String, ProjectMetadata> {
        val metadataMap = mutableMapOf<String, ProjectMetadata>()
        val projectMap = mutableMapOf<String, Project>()

        // Collect all projects
        rootProject.allprojects.forEach { project ->
            projectMap[project.path] = project
        }

        // Build metadata recursively for each project
        projectMap.forEach { (path, project) ->
            buildMetadataRecursively(project, projectMap, metadataMap)
        }

        return metadataMap
    }

    /**
     * Recursively builds ProjectMetadata for a project and its dependencies.
     */
    private fun buildMetadataRecursively(
        project: Project,
        projectMap: Map<String, Project>,
        metadataMap: MutableMap<String, ProjectMetadata>
    ): ProjectMetadata {
        // Return cached metadata if already built
        metadataMap[project.path]?.let {
            return it
        }

        // Find dependency paths
        val dependencyPaths = findProjectDependencies(project)

        // Recursively build metadata for each dependency
        val dependencyMetadataList = dependencyPaths.mapNotNull { depPath ->
            projectMap[depPath]?.let { depProject ->
                buildMetadataRecursively(depProject, projectMap, metadataMap)
            }
        }

        // Create metadata with dependencies
        val metadata = ProjectMetadata(
            name = project.name,
            fullyQualifiedName = project.path,
            dependencies = dependencyMetadataList
        )

        // Cache the metadata
        metadataMap[project.path] = metadata

        return metadata
    }

    /**
     * Builds a ProjectMetadata tree for a specific project.
     *
     * @param project The Gradle project to build metadata for
     * @return ProjectMetadata for the specified project
     */
    fun buildProjectMetadata(project: Project): ProjectMetadata {
        val metadataMap = buildProjectMetadataMap(project.rootProject)
        return metadataMap[project.path] ?: ProjectMetadata(
            name = project.name,
            fullyQualifiedName = project.path,
            dependencies = emptyList()
        )
    }

    /**
     * Finds all project paths that the given project depends on.
     *
     * @param project The project to find dependencies for
     * @return Set of project paths that are dependencies
     */
    private fun findProjectDependencies(project: Project): Set<String> {
        val dependencies = mutableSetOf<String>()

        try {
            project.configurations.forEach { config ->
                config.dependencies
                    .filterIsInstance<ProjectDependency>()
                    .forEach { projectDep ->
                        dependencies.add(projectDep.dependencyProject.path)
                    }
            }
        } catch (e: Exception) {
            // Configuration might not be available yet, skip
            logger.debug("Could not resolve dependencies for ${project.path}: ${e.message}")
        }

        return dependencies
    }
}
