# Test Structure

This project uses separate source sets for different types of tests.

## Directory Structure

```
src/test/
├── unit/
│   └── kotlin/
│       └── com/bitmoxie/monorepochangedprojects/
│           ├── domain/
│           ├── git/
│           └── ... (unit test files)
└── functional/
    └── kotlin/
        └── com/bitmoxie/monorepochangedprojects/
            └── ... (functional test files)
```

## Test Types

### Unit Tests (`src/test/unit/`)
Unit tests focus on testing individual components in isolation:
- Domain model tests (ProjectMetadata, ChangedProjects)
- Service/utility class tests
- Fast execution
- No external dependencies

### Functional Tests (`src/test/functional/`)
Functional tests verify end-to-end functionality:
- Plugin integration tests
- Full workflow tests
- May use actual Gradle projects
- Test real-world scenarios

## Running Tests

### Run All Tests
```bash
./gradlew check
```
This runs both unit and functional tests.

### Run Only Unit Tests
```bash
./gradlew unitTest
```

### Run Only Functional Tests
```bash
./gradlew functionalTest
```

### Run Tests in Specific Order
Functional tests automatically run after unit tests when using `check`.

## Adding New Tests

### Adding a Unit Test
Create your test file in:
```
src/test/unit/kotlin/com/bitmoxie/monorepochangedprojects/YourTest.kt
```

### Adding a Functional Test
Create your test file in:
```
src/test/functional/kotlin/com/bitmoxie/monorepochangedprojects/YourFunctionalTest.kt
```

## Test Configuration

Both test types:
- Use JUnit Platform (Kotest)
- Use the same test dependencies
- Run with `outputs.upToDateWhen { false }` to always execute
- Have access to main source set output

## Benefits of Separation

1. **Faster Feedback**: Run quick unit tests first
2. **Clear Organization**: Easy to find tests by type
3. **Selective Execution**: Run only the tests you need
4. **Better CI**: Can run unit tests in parallel, functional tests separately
5. **Clearer Intent**: Test names and locations indicate their purpose
