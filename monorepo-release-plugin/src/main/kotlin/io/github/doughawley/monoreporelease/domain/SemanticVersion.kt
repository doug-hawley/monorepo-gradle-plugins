package io.github.doughawley.monoreporelease.domain

data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<SemanticVersion> {

    fun bump(scope: Scope): SemanticVersion {
        return when (scope) {
            Scope.MAJOR -> SemanticVersion(major + 1, 0, 0)
            Scope.MINOR -> SemanticVersion(major, minor + 1, 0)
            Scope.PATCH -> SemanticVersion(major, minor, patch + 1)
        }
    }

    override fun compareTo(other: SemanticVersion): Int {
        val majorCmp = major.compareTo(other.major)
        if (majorCmp != 0) return majorCmp
        val minorCmp = minor.compareTo(other.minor)
        if (minorCmp != 0) return minorCmp
        return patch.compareTo(other.patch)
    }

    override fun toString(): String {
        return "$major.$minor.$patch"
    }

    companion object {
        fun parse(raw: String): SemanticVersion? {
            val cleaned = raw.trimStart('v')
            val parts = cleaned.split(".")
            if (parts.size != 3) return null
            val major = parts[0].toIntOrNull() ?: return null
            val minor = parts[1].toIntOrNull() ?: return null
            val patch = parts[2].toIntOrNull() ?: return null
            return SemanticVersion(major, minor, patch)
        }
    }
}
