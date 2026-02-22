# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Gradle plugin (Kotlin) that optimizes CI/CD build times in multi-module projects by detecting which Gradle projects have changed based on git history, including transitive dependents.

## Build & Test Commands

```bash
./gradlew :monorepo-build-plugin:build                  # Full build
./gradlew :monorepo-build-plugin:unitTest               # Unit tests only
./gradlew :monorepo-build-plugin:functionalTest         # Functional/integration tests only
./gradlew :monorepo-build-plugin:check                  # All tests + validation
./gradlew :monorepo-build-plugin:publishToMavenLocal    # Publish to local Maven repo
./gradlew :monorepo-build-plugin:validatePlugins        # Validate plugin descriptor
```

To run a single test class, use the `--tests` filter:
```bash
./gradlew :monorepo-build-plugin:unitTest --tests "io.github.doughawley.monorepobuild.GitChangedFilesDetectorTest"
./gradlew :monorepo-build-plugin:functionalTest --tests "io.github.doughawley.monorepobuild.functional.MonorepoPluginFunctionalTest"
```

After running tests, check results in `monorepo-build-plugin/build/reports/tests/*/index.html` or `monorepo-build-plugin/build/test-results/*/*.xml` for details.

## Architecture

The plugin follows a data-flow pipeline executed during the Gradle configuration phase (`projectsEvaluated`):

```
GitChangedFilesDetector  →  ProjectFileMapper  →  ProjectMetadataFactory  →  Extension / Tasks
(git diff/ls-files)          (file → project)       (dependency graph)         (results stored)
```

**Key classes** (all under `monorepo-build-plugin/src/main/kotlin/io/github/doughawley/monorepobuild/`):

| Class | Role |
|---|---|
| `MonorepoBuildPlugin` | Plugin entry point; registers extension and tasks; triggers metadata computation in `projectsEvaluated` |
| `MonorepoBuildExtension` | User configuration DSL (`baseBranch`, `includeUntracked`, `excludePatterns`) and internal metadata storage |
| `PrintChangedProjectsTask` | Reads pre-computed metadata from extension and outputs results |
| `GitChangedFilesDetector` | Runs `git diff`, `git diff --cached`, and `git ls-files` to find changed files; applies exclude patterns |
| `ProjectFileMapper` | Maps changed file paths to Gradle project paths |
| `ProjectMetadataFactory` | Builds dependency graph by introspecting Gradle `ProjectDependency` objects |
| `domain/ProjectMetadata` | Immutable data model; `hasChanges()` traverses transitive deps |
| `domain/ChangedProjects` | Query API over the metadata map (prefix filtering, summaries) |
| `git/GitCommandExecutor` | Low-level `ProcessBuilder` wrapper for executing git commands |

**Root project special case**: The root project is marked as changed only when files in the root directory (not inside any subproject directory) have changed.

**Results are stored** in `project.extensions.extraProperties` under keys `changedProjects`, `changedProjectsMetadata`, and `changedFilesMap` for use by downstream tasks.

## Test Structure

- **Unit tests**: `monorepo-build-plugin/src/test/unit/kotlin/` — fast, isolated, mock-free Kotest tests
- **Functional tests**: `monorepo-build-plugin/src/test/functional/kotlin/` — Gradle TestKit tests that create real temporary projects with git repositories

The functional tests use a standard 5-module dependency tree (`common-lib` ← `module1`, `module2` ← `app1`, `app2`) created by `StandardTestProject` and `TestProjectBuilder`.

## Code Style

- Always use block bodies with `{}` and explicit `return`; never expression bodies with `=`
- Prefer `val` over `var`; return empty collections instead of null
- Extract private methods >20 lines into separate focused classes
- Use `ProcessBuilder` (never `Runtime.exec()`) with exit code checks for all external processes
- Normalize file paths for cross-platform comparison (trailing slashes, `relativeTo()`)

## Testing Standards

Use Kotest FunSpec with Given/When/Then comments and Kotest matchers:

```kotlin
class MyTest : FunSpec({
    test("should do X when Y") {
        // given
        val input = ...
        // when
        val result = subject.process(input)
        // then
        result shouldBe expected
    }
})
```

Never use JUnit or `assertEquals` — use `shouldBe`, `shouldContain`, `shouldBeInstanceOf<T>()`, etc.

## Documentation & File Discipline

Only maintain `README.md`, `CHANGELOG.md`, and `CLAUDE.md`. Do not create summary files, migration guides, or status reports. Use KDoc for public APIs; keep inline comments minimal.

## Release Process

1. Update version in `monorepo-build-plugin/build.gradle.kts`
2. Update `CHANGELOG.md`
3. Commit, then tag: `git tag v1.x.x && git push origin v1.x.x`
4. GitHub Actions creates the release automatically
