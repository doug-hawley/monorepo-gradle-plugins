# Code Review Issues - To Be Resolved

**Review Date**: February 12, 2026  
**Total Issues**: 11 (2 Critical, 3 High Priority, 4 Medium Priority, 2 Low Priority)

---

## Issue #2 - CRITICAL 🔴
**Status**: ❌ Not Fixed  
**Priority**: Critical  
**File**: `MonorepoChangedProjectsPlugin.kt`, lines 26-28  
**Type**: Task Execution Order

### Problem
The `buildChangedProjects` task depends on `detectChangedProjects` using `dependsOn`, but reads from `extraProperties` in `doLast`. Task execution order is not guaranteed to complete before property access.

```kotlin
doLast {
    val changedProjects = project.extensions.extraProperties.get("changedProjects") as Set<String>
```

### Why It's Critical
- Race condition possible
- Property might not exist if dependency task hasn't completed
- Could fail silently or with cryptic error

### Recommended Fix
Use task outputs/inputs for safer task communication, or explicitly check task completion:

```kotlin
project.tasks.register("buildChangedProjects").configure {
    group = "build"
    description = "Builds only the projects that have been affected by changes"
    
    // Ensure detectChangedProjects completes first
    mustRunAfter("detectChangedProjects")
    
    doLast {
        // Check if the required property exists
        if (!project.extensions.extraProperties.has("changedProjects")) {
            project.logger.error("detectChangedProjects must run before buildChangedProjects")
            throw IllegalStateException("Changed projects data not available. Run detectChangedProjects first.")
        }
        
        val changedProjects = project.extensions.extraProperties.get("changedProjects") as? Set<String> ?: emptySet()
        // ...rest of implementation
    }
}
```

### Testing
- Try running `buildChangedProjects` in isolation
- Verify clear error message
- Test with `--parallel` execution

---

## Issue #3 - CRITICAL 🔴
**Status**: ❌ Not Fixed  
**Priority**: Critical  
**File**: `MonorepoChangedProjectsPlugin.kt`, lines 42-46  
**Type**: Incorrect Task Execution

### Problem
Manually executing task actions bypasses Gradle's task execution engine.

```kotlin
val buildTask = targetProject.tasks.findByName("build")
if (buildTask != null) {
    buildTask.actions.forEach { action ->
        action.execute(buildTask)
    }
}
```

### Why It's Critical
This completely breaks Gradle's task execution model:
- Task dependencies won't be executed (e.g., `compileKotlin` won't run before `build`)
- Task inputs/outputs won't be validated
- Task caching won't work
- Up-to-date checks won't run
- Incremental builds won't work
- Task ordering is ignored

### Recommended Fix
Use proper Gradle task dependency mechanisms:

**Option 1: Use GradleBuild task (recommended)**
```kotlin
project.tasks.register("buildChangedProjects", GradleBuild::class.java).configure {
    group = "build"
    description = "Builds only the projects that have been affected by changes"
    
    doFirst {
        val changedProjects = project.extensions.extraProperties.get("changedProjects") as? Set<String> ?: emptySet()
        if (changedProjects.isEmpty()) {
            project.logger.lifecycle("No projects have changed - nothing to build")
        } else {
            tasks = changedProjects.map { "$it:build" }
            project.logger.lifecycle("Building ${changedProjects.size} changed project(s)")
        }
    }
}
```

**Option 2: Use dependsOn dynamically**
```kotlin
project.tasks.register("buildChangedProjects").configure {
    group = "build"
    description = "Builds only the projects that have been affected by changes"
    dependsOn("detectChangedProjects")
    
    doFirst {
        val changedProjects = project.extensions.extraProperties.get("changedProjects") as? Set<String> ?: emptySet()
        
        if (changedProjects.isEmpty()) {
            project.logger.lifecycle("No projects have changed - nothing to build")
        } else {
            // Add dynamic dependencies
            val buildTasks = changedProjects.mapNotNull { projectPath ->
                project.findProject(projectPath)?.tasks?.findByName("build")
            }
            dependsOn(buildTasks)
        }
    }
}
```

### Testing
- Verify task dependencies execute (e.g., compilation happens)
- Test task caching behavior
- Verify up-to-date checking works
- Test incremental builds

---

## Issue #4 - HIGH PRIORITY 🟡
**Status**: ❌ Not Fixed  
**Priority**: High  
**File**: `ProjectMetadata.kt`, lines 12-19  
**Type**: Infinite Recursion

### Problem
Circular dependencies will cause stack overflow.

```kotlin
fun hasChanges(): Boolean {
    if (changedFiles.isNotEmpty()) {
        return true
    }
    return dependencies.any { it.hasChanges() }
}
```

### Why It's High Priority
If projects have circular dependencies (A → B → A), this will crash with `StackOverflowError`.

### Recommended Fix
Add visited tracking to prevent infinite recursion:

```kotlin
fun hasChanges(visited: MutableSet<String> = mutableSetOf()): Boolean {
    // Avoid circular dependencies
    if (visited.contains(fullyQualifiedName)) {
        return false
    }
    visited.add(fullyQualifiedName)
    
    // Check if this project has direct changes
    if (changedFiles.isNotEmpty()) {
        return true
    }
    
    // Check if any dependency has changes (recursively)
    return dependencies.any { it.hasChanges(visited) }
}
```

**Note**: This will require updating all callers of `hasChanges()` throughout the codebase.

### Testing
- Create test with circular dependencies: A → B → A
- Create test with transitive circular: A → B → C → A
- Verify no stack overflow
- Verify correct change detection

### Files to Update
- `ProjectMetadata.kt` - Add visited parameter
- `DetectChangedProjectsTask.kt` - Update caller (line 34)
- `ChangedProjects.kt` - Update callers (lines 15, 26, 35, 104, 127, etc.)

---

## Issue #5 - HIGH PRIORITY 🟡
**Status**: ❌ Not Fixed  
**Priority**: High  
**File**: `DetectChangedProjectsTask.kt`, lines 11-13  
**Type**: Task Caching/State

### Problem
Using lazy initialization with `logger` in task properties can cause issues with task caching and reuse.

```kotlin
private val gitDetector by lazy { GitChangedFilesDetector(logger) }
private val projectMapper by lazy { ProjectFileMapper() }
private val metadataFactory by lazy { ProjectMetadataFactory(logger) }
```

### Why It's High Priority
- The `logger` instance might change between task executions
- Lazy initialization in task instances can interfere with Gradle's task caching
- Not following Gradle best practices for task implementation

### Recommended Fix
Create instances in the task action method:

```kotlin
abstract class DetectChangedProjectsTask : DefaultTask() {

    @TaskAction
    fun detectChanges() {
        val extension = project.extensions.getByType(ProjectsChangedExtension::class.java)
        
        // Create instances fresh for each execution
        val gitDetector = GitChangedFilesDetector(logger)
        val projectMapper = ProjectFileMapper()
        val metadataFactory = ProjectMetadataFactory(logger)

        logger.lifecycle("Detecting changed projects...")
        // ...rest of implementation
    }
    
    // ...existing code...
}
```

### Testing
- Test with Gradle configuration cache enabled
- Test with build cache enabled
- Verify task works correctly when reused

---

## Issue #6 - HIGH PRIORITY 🟡
**Status**: ❌ Not Fixed  
**Priority**: High  
**File**: `MonorepoChangedProjectsPlugin.kt`, line 50  
**Type**: Error Handling

### Problem
When a project is not found, it's only logged as a warning but execution continues silently.

```kotlin
} else {
    project.logger.warn("Project not found: $projectPath")
}
```

### Why It's High Priority
- Incomplete builds could succeed without clear indication
- Users might not notice missing builds in CI logs
- Could lead to deployment of incomplete/broken code

### Recommended Fix
Make this a hard error or at least track failures:

```kotlin
val missingProjects = mutableListOf<String>()

changedProjects.forEach { projectPath ->
    val targetProject = project.findProject(projectPath)
    if (targetProject != null) {
        // ...build logic
    } else {
        missingProjects.add(projectPath)
        project.logger.error("Project not found: $projectPath")
    }
}

if (missingProjects.isNotEmpty()) {
    throw IllegalStateException(
        "Failed to find ${missingProjects.size} project(s): ${missingProjects.joinToString(", ")}"
    )
}
```

### Testing
- Test with invalid project path in changed projects
- Verify build fails with clear error message
- Test error message format is helpful

---

## Issue #7 - MEDIUM PRIORITY 🟢
**Status**: ❌ Not Fixed  
**Priority**: Medium  
**File**: `ProjectFileMapper.kt`, lines 38-39, 54-56  
**Type**: Path Normalization

### Problem
File paths from git might not be normalized, leading to comparison failures on Windows.

```kotlin
val normalizedProjectPath = projectPath.replace('\\', '/')
// ...
return file.startsWith(normalizedProjectPath)
```

### Why It's Medium Priority
- Could cause incorrect project detection on Windows
- Git typically returns forward slashes, but not guaranteed
- Could cause false negatives (changes not detected)

### Recommended Fix
Normalize both paths before comparison:

```kotlin
private fun isFileInProject(file: String, normalizedProjectPath: String, rootProject: Project): Boolean {
    // Normalize file path as well
    val normalizedFile = file.replace('\\', '/')
    
    // For root project, only match files in root directory (not in subprojects)
    if (normalizedProjectPath.isEmpty()) {
        // Check if file is in root and not in any subproject directory
        val isInSubproject = rootProject.subprojects.any { sub ->
            val subPath = sub.projectDir.relativeTo(rootProject.rootDir).path.replace('\\', '/')
            normalizedFile.startsWith("$subPath/")
        }
        return !isInSubproject
    } else {
        // For subprojects, match if file is in project directory
        return normalizedFile.startsWith(normalizedProjectPath)
    }
}
```

### Testing
- Test on Windows with mixed path separators
- Test with git bash vs Windows cmd
- Verify all changed files are correctly mapped

---

## Issue #8 - MEDIUM PRIORITY 🟢
**Status**: ❌ Not Fixed  
**Priority**: Medium  
**File**: `GitChangedFilesDetector.kt`, lines 78-89  
**Type**: Error Handling

### Problem
First exception when trying `origin/branch` is swallowed completely. Users won't know why the primary method failed.

```kotlin
} catch (e: Exception) {
    // If origin/ doesn't work, try local branch comparison
    try {
        gitExecutor.executeForOutput(...)
    } catch (e2: Exception) {
        logger.warn("Could not compare to branch $baseBranch: ${e2.message}")
        emptySet()
    }
}
```

### Why It's Medium Priority
- Makes debugging difficult
- Users don't know if remote is missing, misconfigured, etc.
- Could hide real problems

### Recommended Fix
Log the first exception at debug level:

```kotlin
} catch (e: Exception) {
    logger.debug("Could not compare with origin/$branchRef: ${e.message}")
    // If origin/ doesn't work, try local branch comparison
    try {
        gitExecutor.executeForOutput(
            gitDir,
            "diff", "--name-only", "$baseBranch...HEAD"
        ).toSet()
    } catch (e2: Exception) {
        logger.warn("Could not compare to branch $baseBranch with either origin/ or local: ${e2.message}")
        logger.debug("Original error: ${e.message}")
        emptySet()
    }
}
```

### Testing
- Test with no remote configured
- Test with non-existent branch
- Verify appropriate log messages at each level

---

## Issue #9 - MEDIUM PRIORITY 🟢
**Status**: ❌ Not Fixed  
**Priority**: Medium  
**File**: `ProjectMetadataFactory.kt`, lines 120-135  
**Type**: Fragile Reflection

### Problem
Using reflection to unwrap platform dependencies is fragile and could break with Gradle version changes.

```kotlin
val wrappedDep = dep.javaClass.methods
    .find { it.name == "getDependency" }
    ?.invoke(dep)
```

### Why It's Medium Priority
- Could break with future Gradle versions
- No version checking or validation
- Silent failures might miss dependencies

### Recommended Fix
Add defensive checks and better error handling:

```kotlin
try {
    val method = dep.javaClass.methods.find { it.name == "getDependency" }
    if (method != null && method.parameterCount == 0) {
        val wrappedDep = method.invoke(dep)
        if (wrappedDep is ProjectDependency) {
            dependencies.add(wrappedDep.dependencyProject.path)
            logger.debug("Unwrapped platform dependency: ${wrappedDep.dependencyProject.path}")
        }
    }
} catch (e: Exception) {
    // Don't fail, just log - reflection might not work in all Gradle versions
    logger.debug("Could not unwrap dependency for ${project.path}: ${e.javaClass.simpleName}")
}
```

### Testing
- Test with platform dependencies
- Test with BOM dependencies
- Test across different Gradle versions (7.x, 8.x)
- Verify dependencies are correctly detected

---

## Issue #10 - MEDIUM PRIORITY 🟢
**Status**: ❌ Not Fixed  
**Priority**: Medium  
**File**: `ProjectMetadataFactory.kt`, lines 112-117  
**Type**: Error Handling

### Problem
All configuration resolution failures are silently skipped with debug logging. Real problems might be missed.

```kotlin
} catch (e: Exception) {
    // Individual configuration might not be resolvable, skip
    logger.debug("Could not resolve configuration ${config.name}...")
}
```

### Why It's Medium Priority
- Important dependencies might be missed due to real errors
- Hard to debug when dependencies aren't detected
- No distinction between expected and unexpected failures

### Recommended Fix
Distinguish between expected and unexpected errors:

```kotlin
} catch (e: Exception) {
    when (e) {
        is org.gradle.api.UnknownConfigurationException,
        is org.gradle.api.InvalidUserDataException -> {
            // Expected - some configurations aren't meant to be resolved
            logger.debug("Skipping non-resolvable configuration ${config.name} for ${project.path}")
        }
        else -> {
            // Unexpected - might indicate a real problem
            logger.warn("Unexpected error resolving configuration ${config.name} for ${project.path}: ${e.message}")
            logger.debug("Stack trace:", e)
        }
    }
}
```

### Testing
- Test with various configuration types (api, implementation, testImplementation)
- Test with unresolvable configurations
- Verify warnings appear for real errors

---

## Issue #11 - LOW PRIORITY 🔵
**Status**: ❌ Not Fixed  
**Priority**: Low  
**File**: `ProjectMetadataFactory.kt`, line 76  
**Type**: Code Style

### Problem
Cached value is stored then immediately returned - could be more concise.

```kotlin
// Cache the metadata
metadataMap[project.path] = metadata

return metadata
```

### Recommended Fix
Use Kotlin's `also` for more idiomatic code:

```kotlin
return metadata.also { metadataMap[project.path] = it }
```

### Testing
- No testing needed, purely stylistic change
- Verify functionality remains the same

---

## Issue #12 - LOW PRIORITY 🔵
**Status**: ❌ Not Fixed  
**Priority**: Low  
**File**: `GitChangedFilesDetector.kt`, lines 24-27  
**Type**: Error Handling Design

### Problem
Returns empty set for both "not a git repo" and "no changes". Caller can't distinguish between error states and legitimate empty results.

```kotlin
val gitDir = findGitRoot(rootDir) ?: run {
    logger.warn("Not a git repository")
    return emptySet()
}
```

### Why It's Low Priority
- Current behavior is acceptable for most use cases
- Would require API changes across the codebase
- More of an enhancement than a bug

### Recommended Fix (Future Enhancement)
Consider using a Result type or throwing exceptions for error cases:

```kotlin
sealed class GitChangesResult {
    data class Success(val files: Set<String>) : GitChangesResult()
    object NotAGitRepository : GitChangesResult()
    data class GitError(val message: String) : GitChangesResult()
}

fun getChangedFiles(rootDir: File, extension: ProjectsChangedExtension): GitChangesResult {
    val gitDir = findGitRoot(rootDir) ?: return GitChangesResult.NotAGitRepository
    // ...
}
```

### Testing
- Would require updating all callers
- Add tests for different result types

---

## Resolution Checklist

### Critical Issues (Must Fix Before Release)
- [x] ~~Issue #1 - Unsafe casting in buildChangedProjects~~ ✅ **FIXED**
- [ ] Issue #2 - Task execution order
- [ ] Issue #3 - Manual task execution

### High Priority (Should Fix Soon)
- [ ] Issue #4 - Infinite recursion risk
- [ ] Issue #5 - Lazy initialization in tasks
- [ ] Issue #6 - Missing error handling

### Medium Priority (Fix When Convenient)
- [ ] Issue #7 - Path normalization
- [ ] Issue #8 - Exception logging
- [ ] Issue #9 - Reflection fragility
- [ ] Issue #10 - Configuration resolution errors

### Low Priority (Nice to Have)
- [ ] Issue #11 - Code style improvement
- [ ] Issue #12 - Result type design

---

## Notes

- Mark each issue as ✅ Fixed when completed
- Update the CHANGELOG.md after fixing critical/high priority issues
- Add tests for each fix
- Consider creating separate branches for critical fixes vs enhancements
- Run full test suite after each fix

**Last Updated**: February 12, 2026
