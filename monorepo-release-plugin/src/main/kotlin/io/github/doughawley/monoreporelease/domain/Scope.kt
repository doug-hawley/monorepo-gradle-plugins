package io.github.doughawley.monoreporelease.domain

enum class Scope {
    MAJOR, MINOR, PATCH;

    companion object {
        fun fromString(s: String): Scope? {
            return when (s.lowercase()) {
                "major" -> MAJOR
                "minor" -> MINOR
                "patch" -> PATCH
                else -> null
            }
        }
    }
}
