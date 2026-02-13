package io.github.doughawley.monorepochangedprojects

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
     * @param rootProject The root Gradle project
     * @param changedFiles Set of changed file paths relative to root
     * @return Map of project paths to lists of changed files in that project
     */
    fun mapChangedFilesToProjects(rootProject: Project, changedFiles: Set<String>): Map<String, List<String>> {
        val projectToFilesMap = mutableMapOf<String, MutableList<String>>()

        rootProject.allprojects.forEach { subproject ->
            val projectPath = subproject.projectDir.relativeTo(rootProject.rootDir).path

            // Normalize path separators to forward slashes for cross-platform compatibility
            val normalizedProjectPath = projectPath.replace('\\', '/')

            // Add trailing slash for comparison
            val normalizedProjectPathWithSlash = if (normalizedProjectPath.isEmpty() || normalizedProjectPath == ".") "" else "$normalizedProjectPath/"

            changedFiles.forEach { file ->
                if (isFileInProject(file, normalizedProjectPathWithSlash, rootProject)) {
                    projectToFilesMap.getOrPut(subproject.path) { mutableListOf() }.add(file)
                }
            }
        }

        return projectToFilesMap
    }

    private fun isFileInProject(file: String, normalizedProjectPath: String, rootProject: Project): Boolean {
        // For root project, only match files in root directory (not in subprojects)
        if (normalizedProjectPath.isEmpty()) {
            // Check if file is in root and not in any subproject directory
            val isInSubproject = rootProject.subprojects.any { sub ->
                val subPath = sub.projectDir.relativeTo(rootProject.rootDir).path.replace('\\', '/')
                file.startsWith("$subPath/")
            }
            return !isInSubproject
        } else {
            // For subprojects, match if file is in project directory
            return file.startsWith(normalizedProjectPath)
        }
    }
}
