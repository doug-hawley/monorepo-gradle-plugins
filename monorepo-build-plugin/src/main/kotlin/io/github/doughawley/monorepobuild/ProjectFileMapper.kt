package io.github.doughawley.monorepobuild

import org.gradle.api.Project

/**
 * Maps changed files to their containing Gradle projects.
 */
class ProjectFileMapper {

    /**
     * Finds all projects that directly contain changed files.
     *
     * @param rootProject The root Gradle project
     * @param changedFiles Set of changed file paths relative to root
     * @return Set of project paths that contain changed files
     */
    fun findProjectsWithChangedFiles(rootProject: Project, changedFiles: Set<String>): Set<String> {
        return mapChangedFilesToProjects(rootProject, changedFiles).keys
    }

    /**
     * Maps changed files to their containing Gradle projects.
     *
     * Each file is assigned to exactly one project — the deepest directory ancestor
     * that is a Gradle project. This prevents intermediate hierarchy nodes (e.g. an
     * `:apps` container with no build file) from being reported as changed when only
     * files inside a child project (e.g. `:apps:app1`) have changed.
     *
     * @param rootProject The root Gradle project
     * @param changedFiles Set of changed file paths relative to root
     * @return Map of project paths to lists of changed files in that project
     */
    fun mapChangedFilesToProjects(rootProject: Project, changedFiles: Set<String>): Map<String, List<String>> {
        if (changedFiles.isEmpty()) return emptyMap()

        // Build a list of (normalizedDirPath, gradleProjectPath) for all subprojects.
        // Sort by directory path length descending so the deepest (most specific) match is
        // always found first, ensuring a file in :apps:app1 is not also attributed to :apps.
        val subprojectsByDir = rootProject.subprojects.map { subproject ->
            val dirPath = try {
                subproject.projectDir.relativeTo(rootProject.rootDir).path
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException(
                    "Project '${subproject.path}' has a directory (${subproject.projectDir}) " +
                    "that is not inside the root project directory (${rootProject.rootDir}). " +
                    "All subproject directories must be located under the root project directory.",
                    e
                )
            }
            val normalizedDirPath = dirPath.replace('\\', '/') + "/"
            normalizedDirPath to subproject.path
        }.sortedByDescending { it.first.length }

        val projectToFilesMap = mutableMapOf<String, MutableList<String>>()

        changedFiles.forEach { file ->
            val ownerPath = subprojectsByDir
                .firstOrNull { (dirPath, _) -> file.startsWith(dirPath) }
                ?.second
                ?: rootProject.path

            projectToFilesMap.getOrPut(ownerPath) { mutableListOf() }.add(file)
        }

        return projectToFilesMap
    }
}
