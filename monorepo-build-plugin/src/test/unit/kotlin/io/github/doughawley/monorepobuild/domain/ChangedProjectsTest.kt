package io.github.doughawley.monorepobuild.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ChangedProjectsTest : FunSpec({

    test("getChangedProjectPaths returns fully qualified paths of affected projects") {
        // given
        val lib = ProjectMetadata("lib", ":libs:lib", changedFiles = listOf("file1.kt"))
        val service = ProjectMetadata("service", ":services:service", dependencies = listOf(lib))

        val projects = listOf(lib, service)
        val changedProjects = ChangedProjects(projects)

        // when
        val result = changedProjects.getChangedProjectPaths()

        // then - should include both lib (direct change) and service (depends on lib)
        result shouldHaveSize 2
        result shouldContainAll listOf(":libs:lib", ":services:service")
    }

    test("getChangedProjectCount returns count of affected projects") {
        // given
        val lib = ProjectMetadata("lib", ":lib", changedFiles = listOf("file1.kt"))
        val serviceA = ProjectMetadata("service-a", ":service-a", dependencies = listOf(lib))
        val serviceB = ProjectMetadata("service-b", ":service-b")

        val projects = listOf(lib, serviceA, serviceB)
        val changedProjects = ChangedProjects(projects)

        // when
        val count = changedProjects.getChangedProjectCount()

        // then - should include lib (direct change) and service-a (depends on lib)
        count shouldBe 2
    }

    test("getAllProjectPaths returns all fully qualified paths") {
        // given
        val projects = listOf(
            ProjectMetadata("project-a", ":services:project-a"),
            ProjectMetadata("project-b", ":libs:project-b"),
            ProjectMetadata("project-c", ":project-c")
        )
        val changedProjects = ChangedProjects(projects)

        // when
        val result = changedProjects.getAllProjectPaths()

        // then
        result shouldHaveSize 3
        result shouldContainAll listOf(":services:project-a", ":libs:project-b", ":project-c")
    }

    test("findProjectByName finds project by simple name") {
        // given
        val projects = listOf(
            ProjectMetadata("project-a", ":services:project-a"),
            ProjectMetadata("project-b", ":libs:project-b")
        )
        val changedProjects = ChangedProjects(projects)

        // when
        val result = changedProjects.findProjectByName("project-a")

        // then
        result shouldNotBe null
        result!!.name shouldBe "project-a"
        result.fullyQualifiedName shouldBe ":services:project-a"
    }

    test("findProjectByName finds project by fully qualified name") {
        // given
        val projects = listOf(
            ProjectMetadata("project-a", ":services:project-a"),
            ProjectMetadata("project-b", ":libs:project-b")
        )
        val changedProjects = ChangedProjects(projects)

        // when
        val result = changedProjects.findProjectByName(":libs:project-b")

        // then
        result shouldNotBe null
        result!!.name shouldBe "project-b"
        result.fullyQualifiedName shouldBe ":libs:project-b"
    }

    test("findProjectByName returns null for non-existent project") {
        // given
        val projects = listOf(
            ProjectMetadata("project-a", ":project-a")
        )
        val changedProjects = ChangedProjects(projects)

        // when
        val result = changedProjects.findProjectByName("non-existent")

        // then
        result shouldBe null
    }

    test("getProjectsWithDirectChanges returns only projects with changes") {
        // given
        val projects = listOf(
            ProjectMetadata("project-a", ":project-a", changedFiles = listOf("file1.kt")),
            ProjectMetadata("project-b", ":project-b", changedFiles = emptyList()),
            ProjectMetadata("project-c", ":project-c", changedFiles = listOf("file2.kt", "file3.kt"))
        )
        val changedProjects = ChangedProjects(projects)

        // when
        val result = changedProjects.getProjectsWithDirectChanges()

        // then
        result shouldHaveSize 2
        result[0].name shouldBe "project-a"
        result[1].name shouldBe "project-c"
    }

    test("getChangedFileCountByProject returns correct counts") {
        // given
        val projects = listOf(
            ProjectMetadata("project-a", ":project-a", changedFiles = listOf("file1.kt")),
            ProjectMetadata("project-b", ":project-b", changedFiles = emptyList()),
            ProjectMetadata("project-c", ":project-c", changedFiles = listOf("file2.kt", "file3.kt", "file4.kt"))
        )
        val changedProjects = ChangedProjects(projects)

        // when
        val result = changedProjects.getChangedFileCountByProject()

        // then
        result.size shouldBe 2
        result[":project-a"] shouldBe 1
        result[":project-c"] shouldBe 3
    }

    test("getAllChangedFiles returns all unique files") {
        // given
        val projects = listOf(
            ProjectMetadata("project-a", ":project-a", changedFiles = listOf("file1.kt", "file2.kt")),
            ProjectMetadata("project-b", ":project-b", changedFiles = emptyList()),
            ProjectMetadata("project-c", ":project-c", changedFiles = listOf("file3.kt"))
        )
        val changedProjects = ChangedProjects(projects)

        // when
        val result = changedProjects.getAllChangedFiles()

        // then
        result shouldHaveSize 3
        result shouldContainAll listOf("file1.kt", "file2.kt", "file3.kt")
    }

    test("getTotalChangedFilesCount returns sum of all changed files") {
        // given
        val projects = listOf(
            ProjectMetadata("project-a", ":project-a", changedFiles = listOf("file1.kt", "file2.kt")),
            ProjectMetadata("project-b", ":project-b", changedFiles = emptyList()),
            ProjectMetadata("project-c", ":project-c", changedFiles = listOf("file3.kt", "file4.kt", "file5.kt"))
        )
        val changedProjects = ChangedProjects(projects)

        // when
        val count = changedProjects.getTotalChangedFilesCount()

        // then
        count shouldBe 5
    }

    test("hasAnyChanges returns true when projects have changes") {
        // given
        val projects = listOf(
            ProjectMetadata("project-a", ":project-a", changedFiles = listOf("file1.kt")),
            ProjectMetadata("project-b", ":project-b", changedFiles = emptyList())
        )
        val changedProjects = ChangedProjects(projects)

        // then
        changedProjects.hasAnyChanges() shouldBe true
    }

    test("hasAnyChanges returns false when no projects have changes") {
        // given
        val projects = listOf(
            ProjectMetadata("project-a", ":project-a", changedFiles = emptyList()),
            ProjectMetadata("project-b", ":project-b", changedFiles = emptyList())
        )
        val changedProjects = ChangedProjects(projects)

        // then
        changedProjects.hasAnyChanges() shouldBe false
    }

    test("getProjectsDependingOn returns projects that depend on given project") {
        // given
        val commonLib = ProjectMetadata("common-lib", ":common-lib", changedFiles = listOf("file1.kt"))
        val serviceA = ProjectMetadata("service-a", ":service-a", dependencies = listOf(commonLib))
        val serviceB = ProjectMetadata("service-b", ":service-b", dependencies = listOf(commonLib))
        val serviceC = ProjectMetadata("service-c", ":service-c")

        val projects = listOf(commonLib, serviceA, serviceB, serviceC)
        val changedProjects = ChangedProjects(projects)

        // when
        val result = changedProjects.getProjectsDependingOn(":common-lib")

        // then
        result shouldHaveSize 2
        result.map { it.name } shouldContainAll listOf("service-a", "service-b")
    }

    test("getProjectsDependingOn finds transitive dependencies") {
        // given
        val baseLib = ProjectMetadata("base-lib", ":base-lib")
        val commonLib = ProjectMetadata("common-lib", ":common-lib", dependencies = listOf(baseLib))
        val service = ProjectMetadata("service", ":service", dependencies = listOf(commonLib))

        val projects = listOf(baseLib, commonLib, service)
        val changedProjects = ChangedProjects(projects)

        // when
        val result = changedProjects.getProjectsDependingOn(":base-lib")

        // then
        result shouldHaveSize 2
        result.map { it.name } shouldContainAll listOf("common-lib", "service")
    }

    test("getProjectsDependingOn returns empty list when no dependencies exist") {
        // given
        val projectA = ProjectMetadata("project-a", ":project-a")
        val projectB = ProjectMetadata("project-b", ":project-b")

        val projects = listOf(projectA, projectB)
        val changedProjects = ChangedProjects(projects)

        // when
        val result = changedProjects.getProjectsDependingOn("project-a")

        // then
        result.shouldBeEmpty()
    }

    test("getSummary returns correct summary") {
        // given
        val lib = ProjectMetadata("lib", ":lib", changedFiles = listOf("file1.kt", "file2.kt"))
        val service = ProjectMetadata("service", ":service", dependencies = listOf(lib))
        val standalone = ProjectMetadata("standalone", ":standalone", changedFiles = listOf("file3.kt"))

        val projects = listOf(lib, service, standalone)
        val changedProjects = ChangedProjects(projects)

        // when
        val summary = changedProjects.getSummary()

        // then
        summary.totalProjects shouldBe 3
        summary.changedProjects shouldBe 2  // lib and standalone have direct changes
        summary.affectedProjects shouldBe 3  // lib, service (depends on lib), and standalone
        summary.totalChangedFiles shouldBe 3
        summary.projectNames shouldHaveSize 2  // Direct changes: lib and standalone
        summary.projectNames shouldContainAll listOf(":lib", ":standalone")
        summary.affectedProjectNames shouldHaveSize 3  // All affected: lib, service, standalone
        summary.affectedProjectNames shouldContainAll listOf(":lib", ":service", ":standalone")
    }

    test("toString returns formatted summary") {
        // given
        val projects = listOf(
            ProjectMetadata("project-a", ":project-a", changedFiles = listOf("file1.kt")),
            ProjectMetadata("project-b", ":project-b", changedFiles = emptyList()),
            ProjectMetadata("project-c", ":project-c", changedFiles = listOf("file2.kt", "file3.kt"))
        )
        val changedProjects = ChangedProjects(projects)

        // when
        val result = changedProjects.toString()

        // then
        result shouldBe "ChangedProjects(total=3, changed=2, files=3)"
    }

    test("empty project list") {
        // given
        val changedProjects = ChangedProjects(emptyList())

        // then
        changedProjects.getChangedProjectCount() shouldBe 0
        changedProjects.getTotalChangedFilesCount() shouldBe 0
        changedProjects.hasAnyChanges() shouldBe false
    }

    test("getChangedProjectsWithPrefix returns affected projects matching prefix") {
        // given
        val lib = ProjectMetadata("lib1", ":libs:lib1", changedFiles = listOf("file1.kt"))
        val app1 = ProjectMetadata("app1", ":apps:app1", dependencies = listOf(lib))
        val app2 = ProjectMetadata("app2", ":apps:app2", changedFiles = listOf("file2.kt"))
        val service = ProjectMetadata("api", ":services:api", changedFiles = listOf("file3.kt"))

        val projects = listOf(lib, app1, app2, service)
        val changedProjects = ChangedProjects(projects)

        // when
        val appsWithChanges = changedProjects.getChangedProjectsWithPrefix(":apps")

        // then - should include app1 (depends on changed lib) and app2 (direct change)
        appsWithChanges shouldHaveSize 2
        appsWithChanges.map { it.name } shouldContainAll listOf("app1", "app2")
    }

    test("getChangedProjectsWithPrefix excludes projects without changes") {
        // given
        val projects = listOf(
            ProjectMetadata("app1", ":apps:app1", changedFiles = listOf("file1.kt")),
            ProjectMetadata("app2", ":apps:app2", changedFiles = emptyList()),
            ProjectMetadata("app3", ":apps:app3", changedFiles = listOf("file2.kt"))
        )
        val changedProjects = ChangedProjects(projects)

        // when
        val result = changedProjects.getChangedProjectsWithPrefix(":apps")

        // then
        result shouldHaveSize 2
        result.map { it.name } shouldContainAll listOf("app1", "app3")
    }

    test("getChangedProjectsWithPrefix returns empty list when no matches") {
        // given
        val projects = listOf(
            ProjectMetadata("lib1", ":libs:lib1", changedFiles = listOf("file1.kt")),
            ProjectMetadata("service", ":services:api", changedFiles = listOf("file2.kt"))
        )
        val changedProjects = ChangedProjects(projects)

        // when
        val result = changedProjects.getChangedProjectsWithPrefix(":apps")

        // then
        result.shouldBeEmpty()
    }

    test("getChangedProjectNamesWithPrefix returns simple names") {
        // given
        val projects = listOf(
            ProjectMetadata("app1", ":apps:mobile:app1", changedFiles = listOf("file1.kt")),
            ProjectMetadata("app2", ":apps:web:app2", changedFiles = listOf("file2.kt")),
            ProjectMetadata("lib1", ":libs:lib1", changedFiles = listOf("file3.kt"))
        )
        val changedProjects = ChangedProjects(projects)

        // when
        val result = changedProjects.getChangedProjectNamesWithPrefix(":apps")

        // then
        result shouldHaveSize 2
        result shouldContainAll listOf("app1", "app2")
    }

    test("getChangedProjectPathsWithPrefix returns fully qualified paths") {
        // given
        val projects = listOf(
            ProjectMetadata("app1", ":apps:mobile:app1", changedFiles = listOf("file1.kt")),
            ProjectMetadata("app2", ":apps:web:app2", changedFiles = listOf("file2.kt")),
            ProjectMetadata("lib1", ":libs:lib1", changedFiles = listOf("file3.kt"))
        )
        val changedProjects = ChangedProjects(projects)

        // when
        val result = changedProjects.getChangedProjectPathsWithPrefix(":apps")

        // then
        result shouldHaveSize 2
        result shouldContainAll listOf(":apps:mobile:app1", ":apps:web:app2")
    }

    test("getChangedProjectsWithPrefix works with exact match") {
        // given
        val projects = listOf(
            ProjectMetadata("apps", ":apps", changedFiles = listOf("file1.kt")),
            ProjectMetadata("app1", ":apps:app1", changedFiles = listOf("file2.kt"))
        )
        val changedProjects = ChangedProjects(projects)

        // when
        val result = changedProjects.getChangedProjectsWithPrefix(":apps")

        // then
        result shouldHaveSize 2
        result.map { it.fullyQualifiedName } shouldContainAll listOf(":apps", ":apps:app1")
    }

    test("getChangedProjectsWithPrefix is case sensitive") {
        // given
        val projects = listOf(
            ProjectMetadata("app1", ":Apps:app1", changedFiles = listOf("file1.kt")),
            ProjectMetadata("app2", ":apps:app2", changedFiles = listOf("file2.kt"))
        )
        val changedProjects = ChangedProjects(projects)

        // when
        val result = changedProjects.getChangedProjectsWithPrefix(":apps")

        // then
        result shouldHaveSize 1
        result[0].name shouldBe "app2"
    }
})
