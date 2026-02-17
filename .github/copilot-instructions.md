# Copilot Instructions for Projects Changed Plugin

> **Note**: Keep these instructions concise. Remove redundant examples and explanations to prevent file bloat.

## Project Overview

Gradle plugin in Kotlin that detects changed projects based on git history for optimizing CI/CD pipelines in multi-module projects.

## General Guidelines

- **Keep it simple**: Focus on code quality, not excessive documentation
- **Make changes directly**: Update code and tests, don't create summary files
- **Minimal documentation**: Only update README.md and CHANGELOG.md as needed
- **No status reports**: Don't create migration guides, scan reports, or summary files
- **Let the code speak**: Use clear naming and KDoc instead of separate documentation files
- **No completion files**: Never create files like `USERNAME_UPDATE_COMPLETE.md`, `SUMMARY.md`, `CODE_CHANGES.md`, `CONFIGURATION_PHASE_CHANGES.md`, `IMPLEMENTATION_SUMMARY.md`, or similar status/summary/documentation files at the end of tasks. Use the show_content tool to display summaries to the user instead.
- **Never create documentation files**: Do not create files documenting code changes, implementation details, or migration steps. If the user needs a summary, use show_content to display it directly.

## Code Style and Standards

### General Principles
- Follow Kotlin coding conventions and idiomatic Kotlin style
- Use meaningful variable and function names
- Keep functions focused and single-purpose
- Add KDoc comments for public APIs
- Use immutability by default (prefer `val` over `var`)
- Always use braces `{}` for function bodies, never expression bodies with `=`

### Function Body Style

Always use block bodies with braces `{}` and explicit `return` statements, never expression bodies with `=`.

```kotlin
// ❌ BAD: fun getName(): String = "John"
// ✅ GOOD:
fun getName(): String {
    return "John"
}
```

### Class Decomposition and Single Responsibility

Extract non-trivial private methods (>20 lines or complex logic) into separate classes with focused responsibilities.

**Example:**
```kotlin
// ❌ BAD: Large task with complex private methods
abstract class MyTask : DefaultTask() {
    @TaskAction
    fun execute() {
        val data = complexPrivateMethod1()  // 50 lines
        val result = complexPrivateMethod2(data)  // 40 lines
    }
}

// ✅ GOOD: Focused classes
class DataExtractor(private val logger: Logger) {
    fun extract(): Data { /* ... */ }
}

abstract class MyTask : DefaultTask() {
    private val extractor by lazy { DataExtractor(logger) }
    
    @TaskAction
    fun execute() {
        val data = extractor.extract()
    }
}
```

**Naming:** Use descriptive noun phrases based on responsibility (e.g., `GitChangedFilesDetector`, `ProjectFileMapper`)

### Testing Standards
- Use Kotest FunSpec style for all tests
- Use Kotest matchers (`shouldBe`, etc.) not assertions
- Follow Given/When/Then structure with lowercase comments

**Example:**
```kotlin
class MyFeatureTest : FunSpec({
    test("should do something when condition is met") {
        // given
        val input = setupTestData()
        
        // when
        val result = service.process(input)
        
        // then
        result shouldBe expectedValue
    }
})
```

### Gradle Plugin Development
- Use `ProcessBuilder` (not `Runtime.exec()`) with exit code checks
- Log errors with context using Gradle's logging
- Use Gradle's configuration avoidance APIs

### Error Handling
- Check exit codes for external processes
- Return empty collections over null
- Catch specific exceptions, not generic `Exception`

### Git Operations
- Verify git repository exists before commands
- Check exit codes and handle missing branches
- Filter blank lines from output

### Path Handling
- Normalize paths for comparison (trailing slashes)
- Use `relativeTo()` for relative paths
- Handle cross-platform separators

## Project Structure

```
src/
├── main/kotlin/io/github/doughawley/monorepochangedprojects/
│   ├── MonorepoChangedProjectsPlugin.kt    # Main plugin entry point
│   ├── DetectChangedProjectsTask.kt        # Core task implementation
│   └── ProjectsChangedExtension.kt         # Configuration DSL
└── test/kotlin/io/github/doughawley/monorepochangedprojects/
    └── MonorepoChangedProjectsPluginTest.kt    # Kotest-based tests
```

## Key Design Decisions

### 1. Task Registration
Use `.configure {}` syntax for setting task properties:
```kotlin
project.tasks.register("taskName", TaskClass::class.java).configure {
    group = "category"
    description = "Description"
}
```

### 2. Root Project Handling
Root project should only be marked as changed if files in the root directory (not in subprojects) are changed:
```kotlin
if (normalizedProjectPath.isEmpty()) {
    val isInSubproject = project.rootProject.subprojects.any { sub ->
        val subPath = sub.projectDir.relativeTo(project.rootDir).path
        file.startsWith("$subPath/")
    }
    if (!isInSubproject) {
        affectedProjects.add(subproject.path)
    }
}
```

### 3. Git Command Execution
Always use `ProcessBuilder` with proper error handling:
```kotlin
val process = ProcessBuilder(*command)
    .directory(workingDir)
    .redirectErrorStream(true)
    .start()

val exitCode = process.waitFor()
if (exitCode == 0) {
    // Process successful output
} else {
    // Log error with exit code and output
    logger.warn("Command failed with exit code $exitCode: $errorOutput")
}
```

## Dependencies

**Runtime:** Kotlin stdlib, Gradle API  
**Testing:** Kotest (runner, assertions, property testing), Gradle TestKit

## Common Tasks

### Running Tests
```bash
./gradlew test
```

### Building the Plugin
```bash
./gradlew build
```

### Publishing Locally
```bash
./gradlew publishToMavenLocal
```

## CI/CD

GitHub Actions workflows are configured in `.github/workflows/`:

- **ci.yml**: Main CI pipeline that runs on every push and PR
  - Tests on multiple OS (Ubuntu, macOS, Windows) and Java versions (11, 17, 21)
  - Runs unit and functional tests
  - Validates plugin descriptor
  - Uploads test reports as artifacts
  
- **release.yml**: Release workflow triggered by version tags
  - Creates GitHub releases
  - Attaches built JAR artifacts
  - Ready to publish to Gradle Plugin Portal (commented out)
  
- **dependabot.yml**: Automated dependency updates for GitHub Actions and Gradle dependencies

### Creating a Release

1. Update version in `build.gradle.kts`
2. Update `CHANGELOG.md`
3. Commit changes
4. Create and push a tag: `git tag v1.0.0 && git push origin v1.0.0`
5. GitHub Actions will automatically create a release

## When Adding New Features

1. **Write tests first** using Kotest FunSpec style
2. **Add configuration options** to `ProjectsChangedExtension` if needed
3. **Update documentation** in README.md
4. **Add entries** to CHANGELOG.md
5. **Consider backward compatibility** when changing public APIs
6. **Validate with integration tests** if touching git operations

## Code Review Checklist

Before submitting changes, verify:
- [ ] All tests pass (`./gradlew test`)
- [ ] Code compiles without warnings
- [ ] New features have Kotest tests
- [ ] Public APIs have KDoc comments
- [ ] Error cases are handled appropriately
- [ ] Git operations use ProcessBuilder with exit code checks
- [ ] Paths are normalized for comparison
- [ ] README.md is updated if user-facing behavior changes
- [ ] CHANGELOG.md has an entry for the change (version history only)
- [ ] No unnecessary documentation files were created

## Terminal Command Workarounds

Terminal commands frequently hang or don't return output. **Use the `read_file` tool to directly read test report files - this is faster and more reliable than running terminal commands.**

### Verifying Tests - CORRECT APPROACH

**Use `read_file` tool to read test reports directly:**

```
read_file build/reports/tests/functionalTest/index.html
```

Then look for:
- Test count: `<div class="counter">21</div>` under "tests"
- Failures: `<div class="counter">0</div>` under "failures"  
- Success rate: `<div class="percent">100%</div>`

### If You Must Run Commands

**Only if test reports don't exist yet and you need to run tests:**

```bash
# Let the user run it, or use run_in_terminal with isBackground=true
# Then check file modification time to see when tests completed:
stat build/reports/tests/functionalTest/index.html
```

### Decision Making Rules

**ALWAYS:**
- Use `read_file` to read test report HTML/XML files directly
- Check `build/reports/tests/*/index.html` for test results
- Verify compilation by checking if `build/classes/` contains files
- Check artifacts exist: `ls -la build/libs/*.jar`

**NEVER:**
- Rely on terminal stdout from gradle commands
- Assume success from empty output
- Wait for terminal commands that may hang

## Debugging Functional Tests

When functional tests fail, **always check the test results files** for detailed error messages:

**Primary:** `build/reports/tests/functionalTest/index.html` - Shows pass/fail counts and percentages
**Details:** `build/test-results/functionalTest/TEST-*.xml` - Contains full error messages and stack traces

Key files:
- `TEST-io.github.doughawley.monorepochangedprojects.functional.MonorepoPluginFunctionalTest.xml` - Core plugin tests
- `TEST-io.github.doughawley.monorepochangedprojects.functional.BuildChangedProjectsFunctionalTest.xml` - Build task tests

When debugging:
1. **First check HTML report** for overall pass/fail status
2. Read the XML file to see exact failure messages and captured output
3. Look for git command errors (e.g., "fatal: ambiguous argument")
4. Check the "Directly changed projects" and "All affected projects" output
5. Verify file paths and project paths in the output

## Anti-Patterns to Avoid

❌ **Don't use `Runtime.exec()`** - Use `ProcessBuilder` instead
❌ **Don't ignore exit codes** - Always check command results
❌ **Don't use JUnit** - Use Kotest for all tests
❌ **Don't use `assertEquals`** - Use Kotest matchers like `shouldBe`
❌ **Don't hardcode paths** - Use Gradle's path APIs
❌ **Don't swallow exceptions** - Log them with context
❌ **Don't forget null checks** - Use safe calls or explicit null handling

## Kotest Matchers

```kotlin
result shouldBe expected
value shouldNotBe null
instance.shouldBeInstanceOf<Type>()
list shouldContain element
text shouldContain "substring"
```

## Documentation Standards

- Use KDoc for public APIs with `@param` and `@return` tags
- Only maintain README.md, CHANGELOG.md, and copilot-instructions.md
- No migration guides, scan reports, fix summaries, or status reports
- Keep inline comments minimal - prefer self-documenting code

## Version Management

Follow semantic versioning (MAJOR.MINOR.PATCH). Update version in `build.gradle.kts`, document in CHANGELOG.md, and tag releases in git.

## Additional Resources

- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Kotest Documentation](https://kotest.io/)
- [Gradle Plugin Development](https://docs.gradle.org/current/userguide/custom_plugins.html)
- [ProcessBuilder JavaDoc](https://docs.oracle.com/javase/8/docs/api/java/lang/ProcessBuilder.html)
