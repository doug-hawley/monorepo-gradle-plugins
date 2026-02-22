# Contributing to Monorepo Build Plugin

Thank you for your interest in contributing! This document provides guidelines and information for developers who want to contribute to this project.

## Table of Contents

- [Development Setup](#development-setup)
- [Building the Plugin](#building-the-plugin)
- [Running Tests](#running-tests)
- [Code Style](#code-style)
- [Commit Guidelines](#commit-guidelines)
- [Submitting Changes](#submitting-changes)
- [Release Process](#release-process)
- [Publishing](#publishing)

## Development Setup

### Prerequisites

- JDK 17 or higher
- Git
- An IDE with Kotlin support (IntelliJ IDEA recommended)

### Clone the Repository

```bash
git clone https://github.com/doug-hawley/monorepo-gradle-plugins.git
cd monorepo-gradle-plugins
```

### Project Structure

```
monorepo-gradle-plugins/
├── monorepo-build-plugin/
│   ├── src/
│   │   ├── main/kotlin/io/github/doughawley/monorepobuild/
│   │   │   ├── MonorepoBuildPlugin.kt
│   │   │   ├── MonorepoBuildExtension.kt
│   │   │   ├── PrintChangedProjectsTask.kt
│   │   │   ├── GitChangedFilesDetector.kt
│   │   │   ├── ProjectFileMapper.kt
│   │   │   ├── domain/
│   │   │   │   ├── ChangedProjects.kt
│   │   │   │   └── ProjectMetadata.kt
│   │   │   └── git/
│   │   │       └── GitCommandExecutor.kt
│   │   └── test/
│   │       ├── unit/kotlin/           # Unit tests
│   │       └── functional/kotlin/     # Functional tests with Gradle TestKit
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

### Understanding the Architecture

The plugin consists of several key components:

1. **MonorepoBuildPlugin** - Main plugin class that registers tasks and extensions
2. **MonorepoBuildExtension** - Configuration DSL and results storage
3. **GitChangedFilesDetector** - Detects changed files using git commands
4. **ProjectFileMapper** - Maps changed files to Gradle projects
5. **ChangedProjects** - Domain object for convenient access to change information

## Building the Plugin

### Full Build

Build the plugin and run all tests:

```bash
./gradlew :monorepo-build-plugin:build
```

### Quick Build (Skip Tests)

```bash
./gradlew :monorepo-build-plugin:build -x test
```

### Assemble Only

Create the JAR without running tests:

```bash
./gradlew :monorepo-build-plugin:assemble
```

The built plugin JAR will be in `monorepo-build-plugin/build/libs/monorepo-build-plugin-1.1.0.jar`.

## Running Tests

This project uses [Kotest](https://kotest.io/) for testing with separate unit and functional test suites.

### Run All Tests

```bash
./gradlew :monorepo-build-plugin:check
```

### Run Unit Tests Only

```bash
./gradlew :monorepo-build-plugin:unitTest
```

### Run Functional Tests Only

```bash
./gradlew :monorepo-build-plugin:functionalTest
```

### Run Tests with Logging

```bash
./gradlew :monorepo-build-plugin:unitTest --info
```

### Test Structure

- **Unit Tests** (`monorepo-build-plugin/src/test/unit/kotlin/`) - Fast, isolated tests for individual components
- **Functional Tests** (`monorepo-build-plugin/src/test/functional/kotlin/`) - End-to-end tests using Gradle TestKit

For more information on testing, see:
- [TEST_STRUCTURE.md](TEST_STRUCTURE.md)
- [FUNCTIONAL_TESTS_GUIDE.md](FUNCTIONAL_TESTS_GUIDE.md)

## Code Style

### Kotlin Conventions

This project follows [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html):

- Use 4 spaces for indentation
- Use camelCase for functions and variables
- Use PascalCase for classes
- Keep line length under 120 characters
- Use meaningful variable names

### Documentation

- All public APIs must have KDoc comments
- Include `@param` and `@return` tags where appropriate
- Provide code examples for complex functionality

Example:
```kotlin
/**
 * Detects changed files by comparing against a base branch.
 *
 * @param baseBranch The branch to compare against (e.g., "main", "develop")
 * @param includeUntracked Whether to include untracked files in the detection
 * @return List of file paths that have changed
 */
fun detectChangedFiles(baseBranch: String, includeUntracked: Boolean): List<String>
```

## Commit Guidelines

This project uses [Conventional Commits](https://www.conventionalcommits.org/) for automated versioning and changelog generation.

### Commit Message Format

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### Types

- **feat**: New feature (MINOR version bump)
- **fix**: Bug fix (PATCH version bump)
- **docs**: Documentation changes
- **style**: Code style changes (formatting, etc.)
- **refactor**: Code refactoring
- **perf**: Performance improvements
- **test**: Adding or updating tests
- **build**: Build system changes
- **ci**: CI configuration changes
- **chore**: Other changes that don't modify src or test files

### Breaking Changes

Use `!` after the type or add `BREAKING CHANGE:` in the footer to trigger a MAJOR version bump:

```bash
git commit -m "feat!: remove support for Gradle 6

BREAKING CHANGE: Minimum Gradle version is now 7.0"
```

### Examples

```bash
# Feature
git commit -m "feat: add support for custom git branches"

# Bug fix
git commit -m "fix: resolve Windows path separator issue"

# Documentation
git commit -m "docs: update README with examples"

# Refactoring
git commit -m "refactor: extract git command execution to separate class"
```

### Validation

Use the validation script to check your commit message:

```bash
./validate-commit.sh "feat: add new feature"
```

For more details, see [RELEASE_PLEASE_GUIDE.md](RELEASE_PLEASE_GUIDE.md).

## Submitting Changes

### Before Submitting

1. **Run tests**: Ensure all tests pass
   ```bash
   ./gradlew :monorepo-build-plugin:check
   ```

2. **Validate plugin**: Check plugin configuration
   ```bash
   ./gradlew :monorepo-build-plugin:validatePlugins
   ```

3. **Check code style**: Ensure code follows conventions

4. **Update documentation**: Update README if adding new features

5. **Write tests**: Add tests for new functionality

### Pull Request Process

1. **Fork the repository** on GitHub

2. **Create a feature branch**:
   ```bash
   git checkout -b feat/my-new-feature
   ```

3. **Make your changes** following the guidelines above

4. **Commit with conventional commit messages**:
   ```bash
   git commit -m "feat: add my new feature"
   ```

5. **Push to your fork**:
   ```bash
   git push origin feat/my-new-feature
   ```

6. **Create a Pull Request** on GitHub with:
   - Clear description of changes
   - Reference to any related issues
   - Screenshots/examples if applicable

7. **Respond to feedback** from reviewers

### What to Expect

- Code reviews are conducted to maintain quality
- CI checks must pass (tests, validation)
- Maintainers may request changes or clarifications
- Approved PRs are merged and included in the next release

## Release Process

This project uses [Release Please](https://github.com/googleapis/release-please) for automated releases.

### How Releases Work

1. **Make conventional commits** to the main branch
2. **Release Please** automatically creates/updates a release PR
3. **Review the release PR** - check version bump and changelog
4. **Merge the release PR** - triggers automatic publishing

### Version Bumping

- `feat:` commits → MINOR version bump (1.0.0 → 1.1.0)
- `fix:` commits → PATCH version bump (1.0.0 → 1.0.1)
- Breaking changes → MAJOR version bump (1.0.0 → 2.0.0)

### Manual Release

To manually create a release:

```bash
git tag v1.0.0
git push origin v1.0.0
```

This triggers the release workflow.

## Publishing

### Local Publishing (Testing)

Publish to Maven Local for testing:

```bash
./gradlew :monorepo-build-plugin:publishToMavenLocal
```

Then in a test project:
```kotlin
repositories {
    mavenLocal()
}

plugins {
    id("io.github.doug-hawley.monorepo-build-plugin") version "1.1.0"
}
```

### Publishing to Gradle Plugin Portal

Publishing is automated via GitHub Actions. See [PUBLISHING_GUIDE.md](PUBLISHING_GUIDE.md) for details.

**Requirements:**
- `GRADLE_PUBLISH_KEY` secret configured in GitHub
- `GRADLE_PUBLISH_SECRET` secret configured in GitHub

The plugin is automatically published when a release PR is merged.

## Development Tips

### Testing Locally

Create a test multi-module project to test the plugin:

```bash
mkdir test-project
cd test-project
git init
# Create build.gradle.kts with your plugin
./gradlew printChangedProjects
```

### Debugging

Run Gradle with debug logging:

```bash
./gradlew printChangedProjects --debug
```

Run tests with IntelliJ IDEA debugger:
1. Set breakpoints in test or plugin code
2. Right-click test file → Debug

### Working with Git Commands

The plugin executes git commands. To test git behavior:

```bash
# See what git diff returns
git diff --name-only main

# See what git diff --staged returns
git diff --staged --name-only
```

## Need Help?

- **Questions**: Open a [GitHub Discussion](https://github.com/doug-hawley/monorepo-gradle-plugins/discussions)
- **Bugs**: Report via [GitHub Issues](https://github.com/doug-hawley/monorepo-gradle-plugins/issues)

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
