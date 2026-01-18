package com.bitmoxie.monorepochangedprojects

import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.logging.Logger

/**
 * Analyzes project dependencies to find transitive dependents of changed projects.
 */
class DependencyAnalyzer(private val logger: Logger) {

    /**
     * Finds all projects affected by changes, including those that depend on changed projects.
     * Uses transitive dependency analysis to find the complete set of affected projects.
     *
     * @param rootProject The root Gradle project
     * @param directlyChangedProjects Projects with direct file changes
     * @return Set of all affected project paths (direct changes + all dependents)
     */
    fun findAllAffectedProjects(rootProject: Project, directlyChangedProjects: Set<String>): Set<String> {
        val allAffected = directlyChangedProjects.toMutableSet()
        val toProcess = directlyChangedProjects.toMutableSet()

        // Keep finding dependents until no new projects are discovered
        while (toProcess.isNotEmpty()) {
            val currentProject = toProcess.first()
            toProcess.remove(currentProject)

            // Find all projects that depend on the current project
            val dependents = findDependentProjects(rootProject, currentProject)
            dependents.forEach { dependent ->
                if (allAffected.add(dependent)) {
                    // If this is a newly discovered affected project, process its dependents too
                    toProcess.add(dependent)
                }
            }
        }

        return allAffected
    }

    /**
     * Finds all projects that directly depend on the given project.
     *
     * @param rootProject The root Gradle project
     * @param projectPath The project path to find dependents for
     * @return Set of project paths that depend on the given project
     */
    private fun findDependentProjects(rootProject: Project, projectPath: String): Set<String> {
        val dependents = mutableSetOf<String>()

        // Verify the target project exists
        rootProject.findProject(projectPath) ?: return dependents

        rootProject.allprojects.forEach { candidateProject ->
            if (candidateProject.path != projectPath) {
                // Check if candidateProject depends on targetProject
                try {
                    val dependencies = candidateProject.configurations
                        .flatMap { config ->
                            config.dependencies.filter { it is ProjectDependency }
                        }
                        .map { (it as ProjectDependency).dependencyProject.path }

                    if (dependencies.contains(projectPath)) {
                        dependents.add(candidateProject.path)
                    }
                } catch (e: Exception) {
                    // Configuration might not be available yet, skip
                    logger.debug("Could not check dependencies for ${candidateProject.path}: ${e.message}")
                }
            }
        }

        return dependents
    }
}
