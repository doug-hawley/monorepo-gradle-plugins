# Monorepo Changed Projects Plugin

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
    id("com.bitmoxie.monorepo-changed-projects") version "1.0.0"
}
```

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

### Access changed projects in other tasks

```kotlin
tasks.register("customTask") {
    dependsOn("detectChangedProjects")
    doLast {
        val changedProjects = project.extensions.extraProperties.get("changedProjects") as Set<String>
        println("Changed projects: $changedProjects")
        
        // Filter tasks to run only for changed projects
        changedProjects.forEach { projectPath ->
            println("Building changed project: $projectPath")
        }
    }
}
```

## Examples

### Multi-module project usage

For a multi-module project, the plugin can determine which subprojects are affected:

```kotlin
// In root build.gradle.kts
plugins {
    id("com.bitmoxie.monorepo-changed-projects") version "1.0.0"
}

projectsChanged {
    baseBranch = "develop"
    excludePatterns = listOf(".*\\.md", "\\.github/.*", "docs/.*")
}

tasks.register("buildChangedProjects") {
    dependsOn("detectChangedProjects")
    doLast {
        val changedProjects = project.extensions.extraProperties.get("changedProjects") as Set<String>
        
        if (changedProjects.isEmpty()) {
            println("No projects changed")
        } else {
            changedProjects.forEach { projectPath ->
                val proj = project.findProject(projectPath)
                proj?.let {
                    println("Running tests for $projectPath")
                    it.tasks.findByName("test")?.let { testTask ->
                        // Configure or run tests for changed project
                    }
                }
            }
        }
    }
}
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
                project.findProject(projectPath)?.tasks?.named("test")?.get()?.let {
                    exec {
                        commandLine("./gradlew", "$projectPath:test")
                    }
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

## Building the plugin

```bash
./gradlew build
```

## Running Tests

Tests are written using [Kotest](https://kotest.io/) with the FunSpec style:

```bash
./gradlew test
```

## Publishing locally

```bash
./gradlew publishToMavenLocal
```

## License

MIT License
