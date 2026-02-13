package io.github.doughawley.monorepochangedprojects

import io.github.doughawley.monorepochangedprojects.domain.ProjectMetadata
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
     * @param changedFilesMap Optional map of project paths to their changed files
     * @return Map of project paths to ProjectMetadata objects
     */
    fun buildProjectMetadataMap(
        rootProject: Project,
        changedFilesMap: Map<String, List<String>> = emptyMap()
    ): Map<String, ProjectMetadata> {
        val metadataMap = mutableMapOf<String, ProjectMetadata>()
        val projectMap = mutableMapOf<String, Project>()

        // Collect all projects
        rootProject.allprojects.forEach { project ->
            projectMap[project.path] = project
        }

        // Build metadata recursively for each project
        projectMap.forEach { (path, project) ->
            buildMetadataRecursively(project, projectMap, metadataMap, changedFilesMap)
        }

        return metadataMap
    }

    /**
     * Recursively builds ProjectMetadata for a project and its dependencies.
     */
    private fun buildMetadataRecursively(
        project: Project,
        projectMap: Map<String, Project>,
        metadataMap: MutableMap<String, ProjectMetadata>,
        changedFilesMap: Map<String, List<String>>
    ): ProjectMetadata {
        // Return cached metadata if already built
        metadataMap[project.path]?.let {
            return it
        }

        // Find dependency paths
        val dependencyPaths = findProjectDependencies(project)

        // Recursively build metadata for each dependency (nested objects)
        val dependencyMetadataList = dependencyPaths.mapNotNull { depPath ->
            projectMap[depPath]?.let { depProject ->
                buildMetadataRecursively(depProject, projectMap, metadataMap, changedFilesMap)
            }
        }

        // Get changed files for this project
        val changedFiles = changedFilesMap[project.path] ?: emptyList()

        // Create metadata with nested dependency objects
        val metadata = ProjectMetadata(
            name = project.name,
            fullyQualifiedName = project.path,
            dependencies = dependencyMetadataList,
            changedFiles = changedFiles
        )

        // Cache the metadata
        metadataMap[project.path] = metadata


        return metadata
    }

    /**
     * Builds a ProjectMetadata tree for a specific project.
     *
     * @param project The Gradle project to build metadata for
     * @param changedFilesMap Optional map of project paths to their changed files
     * @return ProjectMetadata for the specified project
     */
    fun buildProjectMetadata(
        project: Project,
        changedFilesMap: Map<String, List<String>> = emptyMap()
    ): ProjectMetadata {
        val metadataMap = buildProjectMetadataMap(project.rootProject, changedFilesMap)
        return metadataMap[project.path] ?: ProjectMetadata(
            name = project.name,
            fullyQualifiedName = project.path,
            dependencies = emptyList(),
            changedFiles = changedFilesMap[project.path] ?: emptyList()
        )
    }

    /**
     * Finds all project paths that the given project depends on.
     * This includes regular project dependencies and platform/BOM dependencies.
     *
     * @param project The project to find dependencies for
     * @return Set of project paths that are dependencies
     */
    private fun findProjectDependencies(project: Project): Set<String> {
        val dependencies = mutableSetOf<String>()

        try {
            project.configurations.forEach { config ->
                try {
                    // Get all dependencies from the configuration
                    config.dependencies.forEach { dep ->
                        when (dep) {
                            is ProjectDependency -> {
                                // Regular project dependency
                                dependencies.add(dep.dependencyProject.path)
                            }
                            else -> {
                                // Check if it's a platform dependency wrapping a ProjectDependency
                                // Platform dependencies have a 'targetConfiguration' that may be null
                                try {
                                    val targetConfiguration = dep.javaClass.methods
                                        .find { it.name == "getTargetConfiguration" }
                                        ?.invoke(dep)

                                    // Try to unwrap the project dependency from platform notation
                                    val wrappedDep = dep.javaClass.methods
                                        .find { it.name == "getDependency" }
                                        ?.invoke(dep)

                                    if (wrappedDep is ProjectDependency) {
                                        dependencies.add(wrappedDep.dependencyProject.path)
                                    }
                                } catch (e: Exception) {
                                    // Not a platform dependency or can't unwrap, skip
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Individual configuration might not be resolvable, skip
                    logger.debug("Could not resolve configuration ${config.name} for ${project.path}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            // Configuration might not be available yet, skip
            logger.debug("Could not resolve dependencies for ${project.path}: ${e.message}")
        }

        return dependencies
    }
}
