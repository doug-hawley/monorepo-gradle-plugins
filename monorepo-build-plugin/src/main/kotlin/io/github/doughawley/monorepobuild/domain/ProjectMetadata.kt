package io.github.doughawley.monorepobuild.domain

data class ProjectMetadata(
    val name: String,
    val fullyQualifiedName: String,
    val dependencies: List<ProjectMetadata> = emptyList(),
    val changedFiles: List<String> = emptyList()
) {
    /**
     * Returns true if this project has changed files OR any of its dependencies have changes (recursively).
     */
    fun hasChanges(): Boolean {
        // Check if this project has direct changes
        if (changedFiles.isNotEmpty()) {
            return true
        }

        // Check if any dependency has changes (recursively)
        return dependencies.any { it.hasChanges() }
    }

    /**
     * Returns true if this project has direct changed files (not including dependency changes).
     */
    fun hasDirectChanges(): Boolean = changedFiles.isNotEmpty()

    override fun toString(): String {
        return "ProjectMetadata(name='$name', fullyQualifiedName='$fullyQualifiedName', dependencies=${dependencies.size}, changedFiles=${changedFiles.size} files)"
    }
}
