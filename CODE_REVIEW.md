# Code Review Findings

Issues identified during code review. Severity: **high**, **medium**, **low**.

---

## High Severity

### 1. Process resource leak in `GitCommandExecutor`
- **File**: `src/main/kotlin/.../git/GitCommandExecutor.kt` (lines 34–75)
- **Issue**: The `Process` object isn't properly cleaned up. Streams should be closed and `process.destroy()` called in a `finally` block in case of exceptions.
- **Status**: Fixed in `fix-process-resource-leak`

### 2. Race condition in plugin initialization
- **File**: `src/main/kotlin/.../MonorepoChangedProjectsPlugin.kt` (lines 34–78)
- **Issue**: The `synchronized(project.rootProject)` wrapper guards the check-and-set, but Gradle's `extraProperties` may not be thread-safe under the hood. An `AtomicBoolean` would be more explicit and reliable.
- **Status**: Partially addressed in PR #26 (synchronized block added); AtomicBoolean refactor still pending

### 3. Fragile reflection-based dependency unwrapping in `ProjectMetadataFactory`
- **File**: `src/main/kotlin/.../ProjectMetadataFactory.kt` (lines 130–140)
- **Issue**: Platform/BOM dependencies are detected via reflection (`getDependency` method lookup). This relies on undocumented Gradle internals, has no null check on the result, and silently fails — meaning BOM-dependent projects may not be detected correctly.
- **Status**: Fixed in `fix-reflection-dependency-unwrapping` — reflection removed; `platform(project(...))` returns the same `ProjectDependency` with platform attributes set, so it is already caught by the `is ProjectDependency` check. Unit test added to prevent regression.

---

## Medium Severity

### 4. Regex compiled inside loop in `GitChangedFilesDetector`
- **File**: `src/main/kotlin/.../GitChangedFilesDetector.kt` (lines 66–70)
- **Issue**: `Regex(pattern)` is compiled for every file × every exclude pattern — O(n×m) with unnecessary garbage. Pre-compile patterns outside the loop.
- **Status**: Open

### 5. No error handling for non-relative project paths in `ProjectFileMapper`
- **File**: `src/main/kotlin/.../ProjectFileMapper.kt` (line 32)
- **Issue**: `projectDir.relativeTo(rootDir)` throws if a subproject's directory is outside the root, with no clear error message for the user.
- **Status**: Open

### 6. Confusing error message when metadata check fails in `buildChangedProjects`
- **File**: `src/main/kotlin/.../MonorepoChangedProjectsPlugin.kt` (lines 86–111)
- **Issue**: If metadata computation was skipped or failed silently, the resulting `IllegalStateException` doesn't surface the root cause.
- **Status**: Open

### 7. Git three-dot diff fallback may not work for local-only branches
- **File**: `src/main/kotlin/.../GitChangedFilesDetector.kt` (lines 73–94)
- **Issue**: `git diff "$baseBranch...HEAD"` doesn't work if the base branch has no remote tracking ref. Local-only branches need explicit handling.
- **Status**: Open

### 8. No instance reuse for `GitCommandExecutor`
- **File**: `src/main/kotlin/.../GitChangedFilesDetector.kt`, `ProjectMetadataFactory.kt`
- **Issue**: A new `GitCommandExecutor` instance is created on every use despite being stateless — minor overhead in large builds.
- **Status**: Fixed in `fix-git-executor-reuse` — `GitCommandExecutor` is now an injectable constructor parameter on `GitChangedFilesDetector` (with a default for backward compatibility); `computeMetadata()` creates one shared instance and passes it in.

---

## Low Severity

### 9. No warning logged when projects are silently excluded by `hasBuildFile()`
- **File**: `src/main/kotlin/.../MonorepoChangedProjectsPlugin.kt`
- **Issue**: Projects without a build file are silently filtered from the affected list with no diagnostic output.
- **Status**: Open

### 10. Inconsistent error handling in `getStagedFiles()` and `getUntrackedFiles()`
- **File**: `src/main/kotlin/.../GitChangedFilesDetector.kt` (lines 109–114)
- **Issue**: These methods lack try-catch blocks unlike other git command methods, so exceptions propagate immediately instead of being caught and logged.
- **Status**: Open

### 11. No test for circular dependency graph handling
- **File**: Test suite
- **Issue**: The `visited` set in `hasDependencyOn()` handles circular deps, but there is no test to confirm this. A corrupted graph could cause an infinite loop.
- **Status**: Open

### 12. No comment explaining root project exclusion
- **File**: `src/main/kotlin/.../MonorepoChangedProjectsPlugin.kt` (lines 145–150)
- **Issue**: The check `metadata.fullyQualifiedName != ":"` silently excludes the root project with no explanation.
- **Status**: Open

### 13. Inconsistent root project path handling
- **File**: `src/main/kotlin/.../ProjectFileMapper.kt` (lines 37–38)
- **Issue**: The empty string / `"."` check for the root project is inconsistent with how the rest of the codebase identifies the root, making the logic hard to follow.
- **Status**: Open

### 14. Hard-coded default base branch of `"main"`
- **File**: `src/main/kotlin/.../ProjectsChangedExtension.kt` (line 12)
- **Issue**: Teams using `master`, `develop`, or other conventions may not notice the default and run against the wrong base branch.
- **Status**: Open

---

## Test Coverage Gaps

### 15. No test for deeply nested projects
- **Issue**: No test covers projects nested multiple levels deep (e.g., `:services:billing:api`), which is common in large monorepos.
- **Status**: Open

### 16. No functional test for exclude patterns end-to-end
- **Issue**: Exclude patterns are covered in unit tests but not verified in a functional test with a real git repo and Gradle build.
- **Status**: Open

### 17. Symlinked project directories not handled
- **File**: `src/main/kotlin/.../ProjectFileMapper.kt` (line 32)
- **Issue**: `relativeTo()` doesn't normalize symlinks. Projects with symlinked directories won't be detected as changed.
- **Status**: Open

### 18. Windows path separator normalization may be incomplete
- **File**: `src/main/kotlin/.../ProjectFileMapper.kt` (line 35)
- **Issue**: Git always returns forward slashes, but certain Windows git configurations may produce inconsistencies that the current normalization doesn't handle.
- **Status**: Open
