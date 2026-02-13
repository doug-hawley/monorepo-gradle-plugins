package io.github.doughawley.monorepochangedprojects.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ProjectMetadataTest : FunSpec({

    test("hasChanges returns true when project has changed files") {
        // given
        val metadata = ProjectMetadata(
            name = "test-project",
            fullyQualifiedName = ":test-project",
            changedFiles = listOf("src/main/File.kt", "build.gradle.kts")
        )

        // then
        metadata.hasChanges() shouldBe true
    }

    test("hasChanges returns false when project has no changed files") {
        // given
        val metadata = ProjectMetadata(
            name = "test-project",
            fullyQualifiedName = ":test-project",
            changedFiles = emptyList()
        )

        // then
        metadata.hasChanges() shouldBe false
    }

    test("hasChanges returns true when dependency has changes") {
        // given
        val dependency = ProjectMetadata(
            name = "dependency",
            fullyQualifiedName = ":dependency",
            changedFiles = listOf("dependency/File.kt")
        )

        val metadata = ProjectMetadata(
            name = "test-project",
            fullyQualifiedName = ":test-project",
            dependencies = listOf(dependency),
            changedFiles = emptyList()
        )

        // then - should return true because dependency has changes
        metadata.hasChanges() shouldBe true
    }

    test("hasChanges returns true when transitive dependency has changes") {
        // given
        val nestedDep = ProjectMetadata(
            name = "nested-dep",
            fullyQualifiedName = ":nested-dep",
            changedFiles = listOf("nested/File.kt")
        )

        val dependency = ProjectMetadata(
            name = "dependency",
            fullyQualifiedName = ":dependency",
            dependencies = listOf(nestedDep),
            changedFiles = emptyList()
        )

        val metadata = ProjectMetadata(
            name = "test-project",
            fullyQualifiedName = ":test-project",
            dependencies = listOf(dependency),
            changedFiles = emptyList()
        )

        // then - should return true because transitive dependency has changes
        metadata.hasChanges() shouldBe true
    }

    test("hasDirectChanges returns true only for direct changes") {
        // given
        val dependency = ProjectMetadata(
            name = "dependency",
            fullyQualifiedName = ":dependency",
            changedFiles = listOf("dependency/File.kt")
        )

        val metadata = ProjectMetadata(
            name = "test-project",
            fullyQualifiedName = ":test-project",
            dependencies = listOf(dependency),
            changedFiles = emptyList()
        )

        // then
        metadata.hasChanges() shouldBe true  // dependency has changes
        metadata.hasDirectChanges() shouldBe false  // but no direct changes
    }

    test("toString includes dependency count and file count") {
        // given
        val dep1 = ProjectMetadata("dep1", ":dep1")
        val dep2 = ProjectMetadata("dep2", ":dep2")

        val metadata = ProjectMetadata(
            name = "test-project",
            fullyQualifiedName = ":test-project",
            dependencies = listOf(dep1, dep2),
            changedFiles = listOf("file1.kt", "file2.kt", "file3.kt")
        )

        // when
        val result = metadata.toString()

        // then
        result shouldBe "ProjectMetadata(name='test-project', fullyQualifiedName=':test-project', dependencies=2, changedFiles=3 files)"
    }
})
