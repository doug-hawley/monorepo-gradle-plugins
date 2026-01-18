package com.bitmoxie.monorepochangedprojects.domain

data class ProjectMetadata(
    val name: String,
    val fullyQualifiedName: String,
    val dependencies: List<ProjectMetadata> = emptyList()
) {
    fun findDependencyByName(name: String): ProjectMetadata? {
        return dependencies.firstOrNull { it.name == name }
    }

    fun findDependencyRecursively(name: String): ProjectMetadata? {
        if (this.name == name || this.fullyQualifiedName == name) {
            return this
        }
        dependencies.forEach { dep ->
            dep.findDependencyRecursively(name)?.let {
                return it
            }
        }
        return null
    }

    override fun toString(): String {
        return "ProjectMetadata(name='$name', fullyQualifiedName='$fullyQualifiedName', dependencies=${dependencies.map { it.name }})"
    }
}
