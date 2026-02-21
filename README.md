# Monorepo Changed Projects Plugin

[![CI](https://github.com/doug-hawley/monorepo-changed-projects-plugin/actions/workflows/ci.yml/badge.svg)](https://github.com/doug-hawley/monorepo-changed-projects-plugin/actions/workflows/ci.yml)

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

## Features

- Detects changed files by comparing against a base branch
- Identifies which Gradle projects are affected by the changes
- Reports dependent projects that are impacted by changes in their dependencies
- Configurable base branch comparison
- Option to include untracked files
- Exclude patterns support (ignore documentation, config files, etc.)
- Works with multi-module projects and monorepos of any size

## Usage

### Apply the plugin

```kotlin
plugins {
    id("io.github.doug-hawley.monorepo-changed-projects-plugin") version "1.1.0" // x-release-please-version
}
```

> **Note:** This plugin uses a GitHub-based plugin ID (`io.github.doug-hawley`) which simplifies verification on the Gradle Plugin Portal without requiring domain ownership.

### Configure the plugin

```kotlin
projectsChanged {
    baseBranch = "main"  // default
    includeUntracked = true  // default
    excludePatterns = listOf(".*\\.md", "docs/.*")
}
```

### Run the detection task

```bash
./gradlew detectChangedProjects
```

### Build only changed projects

The plugin registers a `buildChangedProjects` task that automatically builds only the projects affected by changes:

```bash
./gradlew buildChangedProjects
```

This task will:
1. Detect all changed projects (including those affected by dependency changes)
2. Build each affected project automatically
3. Report which projects were built

### Access changed projects in other tasks

After `detectChangedProjects` runs, the plugin stores results in extra properties for use in downstream tasks:

```kotlin
tasks.register("customTask") {
    dependsOn("detectChangedProjects")
    doLast {
        val changedProjects = project.extensions.extraProperties.get("changedProjects") as Set<String>
        println("Changed projects: $changedProjects")

        changedProjects.forEach { projectPath ->
            println("Affected: $projectPath")
        }
    }
}
```

### Access changed files metadata

```kotlin
tasks.register("showChangedFiles") {
    dependsOn("detectChangedProjects")
    doLast {
        // Map of project path -> list of changed files in that project
        val changedFilesMap = project.extensions.extraProperties.get("changedFilesMap") as Map<String, List<String>>

        changedFilesMap.forEach { (projectPath, files) ->
            println("Project $projectPath has ${files.size} changed files:")
            files.forEach { file ->
                println("  - $file")
            }
        }

        // Full metadata including dependency graph and changed files
        val metadataMap = project.extensions.extraProperties.get("changedProjectsMetadata") as Map<String, io.github.doughawley.monorepochangedprojects.domain.ProjectMetadata>

        metadataMap.values.forEach { metadata ->
            // hasChanges() returns true if project has direct changes OR dependency changes
            if (metadata.hasChanges()) {
                println("${metadata.fullyQualifiedName}: affected by changes")
                println("  Direct changes: ${metadata.changedFiles.size} files")
                println("  Has direct changes: ${metadata.hasDirectChanges()}")
                println("  Dependencies: ${metadata.dependencies.map { it.name }}")
            }
        }
    }
}
```

### Use the ChangedProjects domain object

The `ChangedProjects` domain object provides a richer API over the raw metadata map:

```kotlin
import io.github.doughawley.monorepochangedprojects.domain.ChangedProjects
import io.github.doughawley.monorepochangedprojects.domain.ProjectMetadata

tasks.register("analyzeWithChangedProjects") {
    dependsOn("detectChangedProjects")
    doLast {
        val metadataMap = project.extensions.extraProperties
            .get("changedProjectsMetadata") as Map<String, ProjectMetadata>

        val changedProjects = ChangedProjects(metadataMap.values.toList())

        // Simple list of changed project names
        println("Changed projects: ${changedProjects.getChangedProjects()}")

        // Count
        println("Total changed: ${changedProjects.getChangedProjectCount()} of ${changedProjects.getAllProjects().size}")

        if (changedProjects.hasAnyChanges()) {
            println("Changes detected!")

            // Summary (prints total projects, changed count, affected count, file counts)
            println(changedProjects.getSummary())

            // File counts per project
            changedProjects.getChangedFileCountByProject().forEach { (path, count) ->
                println("$path: $count files")
            }

            // Projects that depend on a changed project
            changedProjects.getChangedProjects().forEach { projectName ->
                val dependents = changedProjects.getProjectsDependingOn(projectName)
                if (dependents.isNotEmpty()) {
                    println("Projects depending on $projectName: ${dependents.map { it.name }}")
                }
            }

            // Filter by path prefix (e.g., all apps)
            val changedApps = changedProjects.getChangedProjectsWithPrefix(":apps")
            println("Changed apps: ${changedApps.map { it.name }}")

            // Or just the paths
            val changedAppPaths = changedProjects.getChangedProjectPathsWithPrefix(":apps")
            println("Changed app paths: $changedAppPaths")
        }
    }
}
```

## Examples

### Multi-module project usage

Apply the plugin in your root `build.gradle.kts` and configure it for your branch convention:

```kotlin
// In root build.gradle.kts
plugins {
    id("io.github.doug-hawley.monorepo-changed-projects-plugin") version "1.1.0" // x-release-please-version
}

projectsChanged {
    baseBranch = "develop"
    excludePatterns = listOf(".*\\.md", "\\.github/.*", "docs/.*")
}
```

Then run the built-in tasks:

```bash
# Detect and print which projects changed
./gradlew detectChangedProjects

# Build only the affected projects
./gradlew buildChangedProjects
```

### CI/CD Integration

Use in CI to only test changed modules:

```kotlin
tasks.register("ciTest") {
    dependsOn("detectChangedProjects")
    doLast {
        val changedProjects = project.extensions.extraProperties.get("changedProjects") as Set<String>

        if (changedProjects.isEmpty()) {
            println("No changes detected, skipping tests")
        } else {
            changedProjects.forEach { projectPath ->
                exec {
                    commandLine("./gradlew", "$projectPath:test")
                }
            }
        }
    }
}
```

### Build Only Changed Apps

Use prefix filtering to build only changed applications:

```kotlin
import io.github.doughawley.monorepochangedprojects.domain.ChangedProjects
import io.github.doughawley.monorepochangedprojects.domain.ProjectMetadata

tasks.register("buildChangedApps") {
    dependsOn("detectChangedProjects")
    doLast {
        val metadataMap = project.extensions.extraProperties
            .get("changedProjectsMetadata") as Map<String, ProjectMetadata>

        val changedProjects = ChangedProjects(metadataMap.values.toList())

        // Get only changed apps (assuming apps are under :apps directory)
        val changedAppPaths = changedProjects.getChangedProjectPathsWithPrefix(":apps")

        if (changedAppPaths.isEmpty()) {
            println("No app changes detected")
        } else {
            println("Building ${changedAppPaths.size} changed apps...")
            changedAppPaths.forEach { appPath ->
                println("Building $appPath")
                exec {
                    commandLine("./gradlew", "$appPath:build")
                }
            }
        }
    }
}
```

## Benefits

### Build Time Optimization

In a monorepo with 50 modules, if only 2 modules have changed:
- **Without this plugin**: Build all 50 modules (~30 minutes)
- **With this plugin**: Build only 2 changed modules + dependents (~5 minutes)
- **Result**: 83% reduction in build time

### CI/CD Cost Savings

- Reduce compute resource usage in CI/CD pipelines
- Faster PR feedback (developers see results sooner)
- Lower cloud infrastructure costs
- More efficient use of build agents

### Developer Experience

- Faster local builds when working on specific modules
- Quick validation that changes don't break dependent modules
- Clear visibility into what parts of the codebase are affected

## How It Works

### Change Detection Process

1. **Git Comparison** - The plugin runs `git diff` to find files changed between the current state and the base branch
2. **Project Mapping** - Maps changed files to their containing Gradle projects
3. **Dependency Analysis** - Identifies projects that depend on the changed projects
4. **Result Aggregation** - Returns a complete set of affected projects

### Example Scenario

Consider a monorepo with this structure:
```
root/
├── common-lib/        (shared utilities)
├── user-service/      (depends on common-lib)
├── order-service/     (depends on common-lib)
└── admin-ui/          (depends on user-service)
```

**If `common-lib` changes:**
- Direct change: `common-lib`
- Dependent projects: `user-service`, `order-service`, `admin-ui`
- **Result**: All 4 projects are marked as changed

**If `order-service` changes:**
- Direct change: `order-service`
- Dependent projects: None
- **Result**: Only `order-service` is marked as changed

This ensures that:
- You build everything that might be affected by a change
- You avoid building projects that are definitely not affected
- You maintain correctness while maximizing efficiency

## Configuration Options

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `baseBranch` | String | `"main"` | The git branch to compare against |
| `includeUntracked` | Boolean | `true` | Whether to include untracked files in detection |
| `excludePatterns` | List<String> | `[]` | Regex patterns for files to exclude |

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
./gradlew detectChangedProjects
```

### No projects detected despite changes

Check your `excludePatterns` configuration - you may be inadvertently excluding files. Enable logging to see what files are being detected:

```bash
./gradlew detectChangedProjects --info
```

### Root project always shows as changed

This is expected if files in the root directory (outside of subproject directories) have changed. To prevent this, ensure all code is within subproject directories.

## Requirements

- Gradle 7.0 or higher
- Git installed and available in PATH
- Java 17 or higher

## Support & Contributions

- **Issues**: Report bugs or request features via [GitHub Issues](https://github.com/doug-hawley/monorepo-changed-projects-plugin/issues)
- **Contributing**: See [CONTRIBUTING.md](CONTRIBUTING.md) for development setup and guidelines
- **Questions**: Start a discussion in [GitHub Discussions](https://github.com/doug-hawley/monorepo-changed-projects-plugin/discussions)

## License

MIT License - see [LICENSE](LICENSE) file for details
