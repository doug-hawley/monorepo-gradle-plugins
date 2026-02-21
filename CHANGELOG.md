# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0](https://github.com/doug-hawley/monorepo-changed-projects-plugin/compare/v1.0.1...v1.1.0) (2026-02-21)


### Features

* Add copilot instructions for Projects Changed Plugin and enhance metadata handling in tasks ([62f2c02](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/62f2c0282bd458f7679e3a85b783931e91973013))
* Repackage plugin to align with new GitHub username and update project structure ([7fc8cb1](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/7fc8cb1b6a2a07e3576ab1b779cc60854a32a398))
* Repackage plugin to align with new GitHub username and update project structure ([f9c2a67](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/f9c2a673d9c4de0f8e8a8a4d48699bc095f46244))


### Bug Fixes

* Address all low-severity code review issues ([#9](https://github.com/doug-hawley/monorepo-changed-projects-plugin/issues/9)–[#14](https://github.com/doug-hawley/monorepo-changed-projects-plugin/issues/14)) ([1582cf7](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/1582cf78b4a3b99da14a48e92620f0fcd92eccfc))
* Address all low-severity code review issues ([#9](https://github.com/doug-hawley/monorepo-changed-projects-plugin/issues/9)–[#14](https://github.com/doug-hawley/monorepo-changed-projects-plugin/issues/14)) ([aa72b85](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/aa72b856467183d557fa86832f677955ac0a0611))
* Address all medium-severity code review issues ([#4](https://github.com/doug-hawley/monorepo-changed-projects-plugin/issues/4)–[#7](https://github.com/doug-hawley/monorepo-changed-projects-plugin/issues/7)) ([10719ac](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/10719ac2c1c13796da5684081e0b164cb8c5ed25))
* Correctly implement buildChangedProjects task using Gradle task graph ([5edffeb](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/5edffeba5be0497b7895e0cfb6a95650e85d286c))
* Correctly implement buildChangedProjects task using Gradle task graph ([cd611fa](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/cd611fa75d8596f14d3b11456b0899cf82c4b796))
* Enhance buildChangedProjects task with property existence check and improved error handling ([fdeff99](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/fdeff9947362da98cabeb0e4bb055203128c432e))
* Ensure git process is always destroyed and interrupt flag is restored ([e6541dd](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/e6541ddd068ca5ffca1d9c0fa0b4cf63a394e390))
* Ensure git process is always destroyed and interrupt flag is restored ([438f3cc](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/438f3cc8461967e4997d62c0cc50230e8b9bd958))
* Guard projectsEvaluated check-and-set with synchronized block ([fa9a116](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/fa9a11686ca8ce2fc507da1b878ec40cea8e147a))
* Guard projectsEvaluated check-and-set with synchronized block ([fb6a396](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/fb6a396ec0e3647e457c5a8360a94c67149988fe))
* Improve safety of buildChangedProjects task by handling null and incorrect types for changedProjects property ([a320ddf](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/a320ddfa3bf124f7925cc7fecb433babc6b4b97f))
* Improve safety of buildChangedProjects task by handling null and… ([0821a7a](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/0821a7acdec829f44a8c8638b7aa8da0a94e5a32))
* New git username ([8ba6f37](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/8ba6f37a23331f4a05778030577c676a6d1116b7))
* Optimize CI configuration to avoid redundant test executions ([c59d7d6](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/c59d7d638103d1c366d778eb9a5b98a3a1c0b72e))
* Optimize CI configuration to avoid redundant test executions ([b4bddb0](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/b4bddb09e632b027c3a8a749a256a736a2ce182c))
* Replace fragile reflection-based platform dependency unwrapping with type check ([4d33ca7](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/4d33ca73c8625d04088f90f2a6e2a20d3532683c))
* Replace fragile reflection-based platform dependency unwrapping with type check ([31c5189](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/31c5189efbe050df070f265c717dd951f82f3233))
* Replace synchronized+extraProperties flag with AtomicBoolean.compareAndSet ([bbd3c91](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/bbd3c91fefdfb4910af04aca2a9ef61c0ad82d93))
* Replace synchronized+extraProperties flag with AtomicBoolean.compareAndSet ([6698dc7](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/6698dc75b14ad477e1f0f287421ca496b0141183))
* Update CI workflow to verify Maven Local path with new group ID ([de27049](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/de27049c98fc1486acf8a61909cd4d8664ea312a))
* Update CI workflow to verify Maven Local path with new group ID ([784faf6](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/784faf64cfd77bf481ad3bce9a179405d2d32259))
* Update plugin ID to it.github.perulish8.monorepo-changed-projects-plugin ([aa64f8e](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/aa64f8e1c24b06b26b68a53dd5744e4d1c1c6d19))


### Documentation

* Add CLAUDE.md with build commands and architecture overview ([428e6e7](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/428e6e75f57dc63532caaa1898089e34cc24e172))
* automate README version bumps and fix stale content ([ddfb8ef](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/ddfb8efac42ed25e21f9165a402e58d7c2a021cb))


### Miscellaneous Chores

* mark code review item [#15](https://github.com/doug-hawley/monorepo-changed-projects-plugin/issues/15) as fixed ([86a32db](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/86a32dbce547e322733e101a54a166d41a9e3a44))
* upgrade com.gradle.plugin-publish to 2.0.0 ([8c72d6e](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/8c72d6e4e635e9fd6d3269a6bebc5f11b6d9d74b))


### Code Refactoring

* Make GitCommandExecutor injectable in GitChangedFilesDetector ([e6c9db8](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/e6c9db81cb3c307aa48d72c644e73f2afc35465b))
* Make GitCommandExecutor injectable in GitChangedFilesDetector ([37a3b18](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/37a3b18c2c06336b566081afe9ef42c1083866a4))


### Tests

* add functional tests for deeply nested project structures ([a0ef684](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/a0ef6846750cce940f64019aae48e56bce59a17d))
* move deeply nested project tests into MonorepoPluginFunctionalTest ([d28d9ac](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/d28d9acefda64293b0ee1d7e2a645d58fea314e4))


### Continuous Integration

* trigger CI on release-please branches ([c1c5a58](https://github.com/doug-hawley/monorepo-changed-projects-plugin/commit/c1c5a583422cb5741ff2300efa240087a099fbbf))

## [1.0.1](https://github.com/Perulish8/monorepo-changed-projects-plugin/compare/v1.0.0...v1.0.1) (2026-02-06)


### Bug Fixes

* Update github action for release publishing ([241bcbb](https://github.com/Perulish8/monorepo-changed-projects-plugin/commit/241bcbb907ad7d96319d88efcd3b33e41eb31cd4))
* Update gradle publishing configuration ([c52b6e8](https://github.com/Perulish8/monorepo-changed-projects-plugin/commit/c52b6e85ef3c77988f0483c4c6438286003bafc4))
* Update plugin ID to com.bitmoxie.monorepo-changed-projects-plugin ([dacd343](https://github.com/Perulish8/monorepo-changed-projects-plugin/commit/dacd343d54991d926c5cc6d7feeeee1518672a11))


### Continuous Integration

* Introduce release please ([4bdc2bc](https://github.com/Perulish8/monorepo-changed-projects-plugin/commit/4bdc2bcdbf954c92bfad64704fa48a5eb8209100))

## [Unreleased]

### Added
- **Release automation** - Integrated Release Please for automated releases based on Conventional Commits
- `RELEASE_PLEASE_GUIDE.md` - Comprehensive guide for using Release Please and Conventional Commits
- `PUBLISHING_GUIDE.md` - Complete guide for publishing to Gradle Plugin Portal with step-by-step instructions
- `LICENSE` - MIT License file for open source distribution
- `.github/workflows/release-please.yml` - GitHub Action workflow for automated releases
- `release-please-config.json` - Release Please configuration
- `.release-please-manifest.json` - Version tracking for Release Please
- **Gradle Plugin Publish plugin** - Added `com.gradle.plugin-publish` plugin (v1.3.0) for compliance with modern Gradle plugin development practices and Plugin Portal publishing
- **Publishing enabled** - Activated publish-plugin job in release.yml workflow for automatic Plugin Portal publishing

### Changed
- **Updated dependencies** - Updated Kotest from 5.8.0 to 5.9.1 and Gradle wrapper from 8.5 to 8.12
- **CI configuration** - Removed Java 11 support (now supporting Java 17 and 21 only)
- **CI configuration** - Removed Windows runners temporarily (now testing on Ubuntu and macOS only)
- **CI configuration** - Optimized test execution to avoid running tests twice (excluded from build task, run explicitly via unitTest and functionalTest tasks)
- **Plugin metadata** - Enhanced `gradlePlugin` configuration with website, vcsUrl, and tags for better Plugin Portal presentation
- **Plugin ID** - Updated to `io.github.doug-hawley.monorepo-changed-projects-plugin` (GitHub-based namespace) for simpler Plugin Portal verification without requiring domain ownership
- **Group ID** - Updated to `io.github.doug-hawley` to align with GitHub-based plugin ID namespace
- **GitHub username** - Updated all references from `perulish8` to `doug-hawley` following GitHub username change
- **Repository URLs** - Updated all GitHub URLs to reflect new username `doug-hawley`
- **Package structure** - Repackaged from `com.bitmoxie.monorepochangedprojects` to `io.github.doughawley.monorepochangedprojects` to align with group ID
- **README.md** - Restructured to focus on users of the plugin rather than developers; moved development instructions to CONTRIBUTING.md

### Fixed
- **Windows path separator compatibility** - Fixed `ProjectFileMapper` to normalize path separators on Windows, ensuring nested projects like `:apps:app2` are correctly identified instead of just their parent directory `:apps`
- **Windows test name compatibility** - Fixed `TestProjectListener` to sanitize test names before using them in temporary directory paths, preventing `InvalidPathException` on Windows when test names contain illegal path characters like colons
- **Release Please configuration** - Fixed `release-please-config.json` to use `generic` type instead of unsupported `gradle` type for version file updates
- **CI workflow** - Fixed Maven Local path verification in CI to match new group ID (`io.github.doug-hawley`)
- **buildChangedProjects task safety** - Fixed unsafe casting in `buildChangedProjects` task to gracefully handle missing, null, or incorrectly-typed `changedProjects` property, preventing runtime crashes

### BREAKING CHANGES
- **Project renamed** from `projects-changed-plugin` to `monorepo-changed-projects-plugin`
- **Plugin ID changed** from `com.bitmoxie.projects-changed` to `io.github.doug-hawley.monorepo-changed-projects-plugin` (using GitHub-based namespace for easier verification)
- **Package renamed** from `com.bitmoxie.projectschanged` to `io.github.doughawley.monorepochangedprojects` to align with group ID
- **Main class renamed** from `ProjectsChangedPlugin` to `MonorepoChangedProjectsPlugin`
- **Migrated tests** from JUnit to Kotest

### Added
- **Dependency tracking** - Plugin now identifies projects that depend on changed projects
- Transitive dependency analysis to find all affected projects in the dependency graph
- Enhanced logging to show both directly changed projects and all affected projects (including dependents)
- **Staged file detection** - Plugin now detects files staged with `git add` but not yet committed
- **`GitChangedFilesDetector`** class - Extracted class handling all git operations for detecting changed files
- **`ProjectFileMapper`** class - Extracted class mapping changed files to their containing Gradle projects
- **`DependencyAnalyzer`** class - Extracted class analyzing project dependencies and finding transitive dependents
- **Comprehensive unit tests for `GitChangedFilesDetector`** (19 tests):
  - Non-git repository handling
  - Exclude pattern functionality
  - Git root discovery in subdirectories
  - Nested git repository handling
  - includeUntracked configuration
  - Multiple exclude patterns
  - Base branch configuration
  - Empty repository handling
  - Error handling and graceful failure
  - **Integration tests with real git operations**:
    - Staged file detection
    - Untracked file detection with configuration
    - Multiple staged and untracked files
    - Exclude pattern application on real files
    - Subdirectory file detection
    - Combined staged and untracked detection
- Unit tests for `ProjectFileMapper` (4 tests covering various scenarios)
- **Comprehensive unit tests for `DependencyAnalyzer`** (12 tests total):
  - Direct dependency detection
  - Two-level transitive dependency chains
  - Deep nested transitive dependencies (5-level chain)
  - Multiple parallel dependency chains
  - Diamond dependency pattern
  - Multiple changed projects with overlapping dependency trees
  - Complex monorepo dependency graph scenarios
  - Isolation testing (unrelated projects not included)
- `copilot-instructions.md` - Comprehensive coding standards and guidelines for contributors
- Documentation of class decomposition standards in copilot-instructions
- Documentation of given/when/then test structure in copilot-instructions
- Kotest dependencies (kotest-runner-junit5, kotest-assertions-core, kotest-property)
- JUnit Platform configuration for Kotest test execution
- README sections: "Overview", "Use Cases", "How It Works", "Benefits"
- Maven publishing configuration for local and remote repositories
- Comprehensive unit tests for plugin functionality
- Tests for default configuration values
- Tests for task group and description
- Test for handling projects without git repository
- Better error messages when git commands fail
- Support for filtering blank lines from git output

### Changed
- **BREAKING**: Migrated tests from JUnit to Kotest
- Tests now use Kotest FunSpec style with idiomatic matchers
- Updated test assertions to use Kotest matchers (`shouldBe`, `shouldNotBe`, etc.)
- **All tests follow given/when/then structure** - Standardized test organization for improved readability
- **Refactored `DetectChangedProjectsTask`** - Decomposed complex private methods into separate focused classes for improved testability and maintainability
- **Refactored `GitChangedFilesDetector`** - Extracted git command execution into `GitCommandExecutor` class for better separation of concerns and testability
- Enhanced README with detailed monorepo optimization use cases and benefits
- Updated task registration to use `.configure {}` syntax for better compatibility
- Replaced `Runtime.getRuntime().exec()` with `ProcessBuilder` for security and reliability
- Git commands now properly wait for process completion before reading output

### Fixed
- **CRITICAL**: Fixed compilation error in `ProjectsChangedPlugin.kt` - incorrect task registration syntax
- **CRITICAL**: Fixed logic bug in `findAffectedProjects()` where root project was incorrectly matching all files
- **CRITICAL**: Fixed missing `getStagedFiles()` method implementation - method was called but not implemented
- Fixed git command execution to use `ProcessBuilder` instead of deprecated `Runtime.exec()`
- Added proper error handling with exit code checking for git commands
- Fixed null check warning in `findGitRoot()` method
- Added proper path normalization to prevent false matches in subproject detection

### Improved
- Enhanced project path matching logic to correctly handle root vs subproject files
- Better logging with error output from failed git commands
- More robust file path comparison with normalized paths
- Improved documentation in README with more examples and troubleshooting steps


## [1.0.0] - Initial Release

### Added
- Initial plugin implementation
- Support for detecting changed files via git diff
- Configuration options for base branch, untracked files, and exclude patterns
- Multi-module project support
- Basic documentation and examples
