# Monorepo Build Plugin

[![CI](https://github.com/doug-hawley/monorepo-gradle-plugins/actions/workflows/ci.yml/badge.svg)](https://github.com/doug-hawley/monorepo-gradle-plugins/actions/workflows/ci.yml)

A Gradle plugin designed to optimize build times in large multi-module Gradle projects and monorepos by detecting which projects have changed based on git history.

## Overview

In large multi-module builds, running the entire build pipeline for every code change is inefficient and time-consuming. This plugin solves that problem by:

1. **Detecting changed files** - Compares the current code state against a base branch to identify modified files
2. **Identifying affected projects** - Determines which Gradle projects contain the changed files
3. **Tracking dependencies** - Reports projects that are affected due to changes in their dependencies
4. **Enabling selective builds** - Provides data to run builds only for projects with changes

This dramatically reduces build times in CI/CD pipelines by avoiding unnecessary compilation, testing, and deployment of unchanged modules.

## Use Cases

- **Monorepo optimization** - Build only affected services/modules in a large monorepo
- **CI/CD efficiency** - Skip tests and builds for unchanged projects
- **Faster feedback loops** - Reduce PR build times by focusing on impacted code
- **Resource optimization** - Save compute resources by avoiding redundant builds

## Usage

### Apply the plugin

```kotlin
plugins {
    id("io.github.doug-hawley.monorepo-build-plugin") version "1.1.0" // x-release-please-version
}
```

### Configure the plugin

```kotlin
monorepoBuild {
    baseBranch = "main"           // branch to compare against for branch-mode tasks; defaults to "main"
    commitRef = "HEAD~1"          // commit SHA, tag, or ref for ref-mode tasks; defaults to "HEAD~1"; can be overridden at runtime via -PmonorepoBuild.commitRef=<sha>
    includeUntracked = true       // include files not yet tracked by git; defaults to true (branch-mode only)
    excludePatterns = listOf(     // regex patterns for files to exclude globally across all projects
        ".*\\.md",
        "docs/.*"
    )
}
```

Individual subprojects can declare their own exclude patterns using the `monorepoProjectConfig` extension. Patterns are matched against paths relative to the subproject directory and are applied after global `excludePatterns`.

```kotlin
// In :api/build.gradle.kts
monorepoProjectConfig {
    excludePatterns = listOf(     // regex patterns relative to this subproject's directory
        "generated/.*",
        ".*\\.json"
    )
}
```

### Tasks

The plugin provides two sets of tasks suited to different workflows:

**Branch-mode tasks** compare against a base branch and are designed for developers working on a feature branch. Before opening a pull request, you can run `buildChangedProjectsFromBranch` to build only the projects you have changed, getting a fast confidence check without rebuilding the entire repository.

**Ref-mode tasks** compare against a specific commit ref and are designed for CI/CD pipelines. When a pipeline wants to build only what changed since the last commit on main, or since a known-good previous commit, ref-mode tasks provide that targeted detection. By default they compare against `HEAD~1` (the previous commit), which works out of the box for pipelines that run on every commit. For pipelines that track a last-known-good SHA, pass that SHA via `-PmonorepoBuild.commitRef=<sha>` to override the default.

#### `printChangedProjectsFromBranch`

Prints a human-readable report of which projects have changed and which are transitively affected, comparing against `baseBranch`.

```bash
./gradlew printChangedProjectsFromBranch
```

#### `buildChangedProjectsFromBranch`

Builds all affected projects (including transitive dependents), comparing against `baseBranch`. Useful before opening a pull request to verify only your changed modules build correctly.

```bash
./gradlew buildChangedProjectsFromBranch
```

#### `printChangedProjectsFromRef`

Prints a human-readable report of which projects changed since a specific commit ref. Defaults to `HEAD~1`.

```bash
# Use the default (HEAD~1)
./gradlew printChangedProjectsFromRef

# Override with a specific SHA
./gradlew printChangedProjectsFromRef -PmonorepoBuild.commitRef=abc123
```

#### `buildChangedProjectsFromRef`

Builds all affected projects since a specific commit ref. Defaults to `HEAD~1`, so it works out of the box for pipelines that trigger on every commit. Override with a specific SHA when your pipeline tracks the last successful build.

```bash
# Build what changed since the previous commit (default)
./gradlew buildChangedProjectsFromRef

# Build what changed since a specific SHA (e.g., last successful CI build)
./gradlew buildChangedProjectsFromRef -PmonorepoBuild.commitRef=abc123def456
```

> **Note:** Ref-mode tasks use a two-dot diff (`git diff <ref> HEAD`), which only considers committed changes. Staged and untracked files are intentionally ignored — this mode is designed for clean CI workspaces.

#### `writeChangedProjectsFromRef`

Writes the list of affected project paths to a file — one path per line, no headers or annotations. Designed for consumption by shell scripts in CI/CD pipelines.

```bash
./gradlew writeChangedProjectsFromRef -PmonorepoBuild.commitRef=abc123
```

**Default output file:** `build/monorepo/changed-projects.txt`

Example output:
```
:common-lib
:modules:module1
:apps:app1
```

An empty file is written when nothing has changed, so downstream scripts can always assume the file exists after the task runs.

**Override the output path at runtime** (no build script changes needed):

```bash
./gradlew writeChangedProjectsFromRef \
  -PmonorepoBuild.commitRef=abc123 \
  -PmonorepoBuild.outputFile=ci/changed-projects.txt
```

**Override the output path permanently in the build script:**

```kotlin
tasks.named<io.github.doughawley.monorepobuild.WriteChangedProjectsFromRefTask>(
    "writeChangedProjectsFromRef"
) {
    outputFile.set(layout.projectDirectory.file("ci/changed-projects.txt"))
}
```

### Access changed projects in other tasks

The plugin computes results during the configuration phase, so any task can access them directly from the `monorepoBuild` extension — no `dependsOn` needed:

```kotlin
tasks.register("customTask") {
    doLast {
        val extension = project.extensions.getByType(
            io.github.doughawley.monorepobuild.MonorepoBuildExtension::class.java
        )
        val changedProjects = extension.allAffectedProjects
        println("Changed projects: $changedProjects")

        changedProjects.forEach { projectPath ->
            println("Affected: $projectPath")
        }
    }
}
```

## Configuration Options

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `baseBranch` | String | `"main"` | The git branch to compare against (branch-mode tasks) |
| `commitRef` | String | `"HEAD~1"` | Commit SHA, tag, or ref expression to compare against HEAD (ref-mode tasks). Can also be supplied at runtime via `-PmonorepoBuild.commitRef=<sha>`, which takes precedence over the DSL value |
| `includeUntracked` | Boolean | `true` | Whether to include untracked files in detection (branch-mode only) |
| `excludePatterns` | List<String> | `[]` | Regex patterns for files to exclude globally |

### Per-project excludes

Individual subprojects can declare their own exclude patterns using the `monorepoProjectConfig` extension. This is useful when a team wants to ignore generated files or other noise that is specific to their module without cluttering the root configuration.

```kotlin
// In :api/build.gradle.kts
monorepoProjectConfig {
    excludePatterns = listOf("generated/.*", ".*\\.json")
}
```

- Patterns are Java regex strings matched against file paths **relative to the subproject directory** (e.g., `generated/Code.kt`, not `api/generated/Code.kt`).
- Per-project patterns are applied **after** global `excludePatterns`, so the two stages are independent and complementary.
- The extension is automatically registered on all subprojects by the plugin — subprojects do not need to apply the plugin themselves.

## Troubleshooting

### "Not a git repository" warning

Ensure you're running the task in a directory that's part of a git repository. The plugin looks for a `.git` directory in the project root or parent directories.

### "Git diff command failed"

This can happen if:
- The base branch doesn't exist locally or remotely
- You haven't fetched the remote branch (`git fetch origin`)
- Git is not installed or not in the PATH

Solution:
```bash
git fetch origin
./gradlew printChangedProjectsFromBranch
```

### No projects detected despite changes

Check your `excludePatterns` configuration - you may be inadvertently excluding files. Enable logging to see what files are being detected:

```bash
./gradlew printChangedProjectsFromBranch --info
```

### Root project always shows as changed

This is expected if files in the root directory (outside of subproject directories) have changed. To prevent this, ensure all code is within subproject directories.

## Requirements

- Gradle 7.0 or higher
- Git installed and available in PATH
- Java 17 or higher

## Support & Contributions

- **Issues**: Report bugs or request features via [GitHub Issues](https://github.com/doug-hawley/monorepo-gradle-plugins/issues)
- **Contributing**: See [CONTRIBUTING.md](CONTRIBUTING.md) for development setup and guidelines
- **Questions**: Start a discussion in [GitHub Discussions](https://github.com/doug-hawley/monorepo-gradle-plugins/discussions)

## License

MIT License - see [LICENSE](LICENSE) file for details
