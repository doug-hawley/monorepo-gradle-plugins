package io.github.doughawley.monorepobuild

/**
 * Per-project extension that allows individual subprojects to declare their own
 * file exclude patterns. Patterns are applied after global excludePatterns and
 * after file-to-project mapping, so they only affect files within the project.
 *
 * Usage in a subproject's build.gradle.kts:
 * ```
 * projectExcludes {
 *     excludePatterns = listOf("generated/.*", ".*\\.json")
 * }
 * ```
 */
open class ProjectExcludesExtension {
    var excludePatterns: List<String> = listOf()
}
