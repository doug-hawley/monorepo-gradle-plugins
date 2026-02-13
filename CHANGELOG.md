# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
