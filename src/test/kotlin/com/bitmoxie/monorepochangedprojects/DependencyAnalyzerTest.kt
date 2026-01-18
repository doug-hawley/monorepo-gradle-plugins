package com.bitmoxie.monorepochangedprojects

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.gradle.testfixtures.ProjectBuilder

class DependencyAnalyzerTest : FunSpec({

    test("should return only directly changed project when no dependents exist") {
        // given
        val rootProject = ProjectBuilder.builder().build()
        ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("standalone")
            .build()
        val logger = rootProject.logger
        val analyzer = DependencyAnalyzer(logger)
        val directlyChanged = setOf(":standalone")

        // when
        val result = analyzer.findAllAffectedProjects(rootProject, directlyChanged)

        // then
        result shouldBe directlyChanged
    }

    test("should return empty set when no projects changed") {
        // given
        val rootProject = ProjectBuilder.builder().build()
        val logger = rootProject.logger
        val analyzer = DependencyAnalyzer(logger)
        val directlyChanged = emptySet<String>()

        // when
        val result = analyzer.findAllAffectedProjects(rootProject, directlyChanged)

        // then
        result.shouldBeEmpty()
    }

    test("should include multiple directly changed projects") {
        // given
        val rootProject = ProjectBuilder.builder().build()
        ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("module-a")
            .build()
        ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("module-b")
            .build()
        val logger = rootProject.logger
        val analyzer = DependencyAnalyzer(logger)
        val directlyChanged = setOf(":module-a", ":module-b")

        // when
        val result = analyzer.findAllAffectedProjects(rootProject, directlyChanged)

        // then
        result.size shouldBe 2
        result shouldContain ":module-a"
        result shouldContain ":module-b"
    }

    test("should handle non-existent project path gracefully") {
        // given
        val rootProject = ProjectBuilder.builder().build()
        val logger = rootProject.logger
        val analyzer = DependencyAnalyzer(logger)
        val directlyChanged = setOf(":nonexistent")

        // when
        val result = analyzer.findAllAffectedProjects(rootProject, directlyChanged)

        // then
        result shouldContain ":nonexistent"
    }

    test("should find direct dependent when one project depends on changed project") {
        // given
        val rootProject = ProjectBuilder.builder().build()
        val commonLib = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("common-lib")
            .build()
        val service = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("user-service")
            .build()

        // Apply Java plugin to create configurations
        commonLib.pluginManager.apply("java-library")
        service.pluginManager.apply("java-library")

        // Create dependency: user-service depends on common-lib
        service.dependencies.add("implementation", commonLib)

        val logger = rootProject.logger
        val analyzer = DependencyAnalyzer(logger)
        val directlyChanged = setOf(":common-lib")

        // when
        val result = analyzer.findAllAffectedProjects(rootProject, directlyChanged)

        // then
        result.size shouldBe 2
        result shouldContain ":common-lib"
        result shouldContain ":user-service"
    }

    test("should find transitive dependents in two-level dependency chain") {
        // given
        val rootProject = ProjectBuilder.builder().build()

        // Build dependency chain: admin-ui -> user-service -> common-lib
        val commonLib = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("common-lib")
            .build()
        val userService = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("user-service")
            .build()
        val adminUi = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("admin-ui")
            .build()

        // Apply Java plugin to create configurations
        commonLib.pluginManager.apply("java-library")
        userService.pluginManager.apply("java-library")
        adminUi.pluginManager.apply("java-library")

        userService.dependencies.add("implementation", commonLib)
        adminUi.dependencies.add("implementation", userService)

        val logger = rootProject.logger
        val analyzer = DependencyAnalyzer(logger)
        val directlyChanged = setOf(":common-lib")

        // when
        val result = analyzer.findAllAffectedProjects(rootProject, directlyChanged)

        // then
        result.size shouldBe 3
        result shouldContain ":common-lib"
        result shouldContain ":user-service"
        result shouldContain ":admin-ui"
    }

    test("should find nested transitive dependents in deep dependency chain") {
        // given
        val rootProject = ProjectBuilder.builder().build()

        // Build deep chain: level4 -> level3 -> level2 -> level1 -> base
        val base = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("base")
            .build()
        val level1 = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("level1")
            .build()
        val level2 = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("level2")
            .build()
        val level3 = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("level3")
            .build()
        val level4 = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("level4")
            .build()

        // Apply Java plugin to create configurations
        base.pluginManager.apply("java-library")
        level1.pluginManager.apply("java-library")
        level2.pluginManager.apply("java-library")
        level3.pluginManager.apply("java-library")
        level4.pluginManager.apply("java-library")

        level1.dependencies.add("implementation", base)
        level2.dependencies.add("implementation", level1)
        level3.dependencies.add("implementation", level2)
        level4.dependencies.add("implementation", level3)

        val logger = rootProject.logger
        val analyzer = DependencyAnalyzer(logger)
        val directlyChanged = setOf(":base")

        // when
        val result = analyzer.findAllAffectedProjects(rootProject, directlyChanged)

        // then
        result.size shouldBe 5
        result shouldContain ":base"
        result shouldContain ":level1"
        result shouldContain ":level2"
        result shouldContain ":level3"
        result shouldContain ":level4"
    }

    test("should find all dependents in multiple dependency chains") {
        // given
        val rootProject = ProjectBuilder.builder().build()

        // Build multiple chains from common-lib:
        // user-service -> common-lib
        // order-service -> common-lib
        val commonLib = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("common-lib")
            .build()
        val userService = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("user-service")
            .build()
        val orderService = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("order-service")
            .build()

        // Apply Java plugin to create configurations
        commonLib.pluginManager.apply("java-library")
        userService.pluginManager.apply("java-library")
        orderService.pluginManager.apply("java-library")

        userService.dependencies.add("implementation", commonLib)
        orderService.dependencies.add("implementation", commonLib)

        val logger = rootProject.logger
        val analyzer = DependencyAnalyzer(logger)
        val directlyChanged = setOf(":common-lib")

        // when
        val result = analyzer.findAllAffectedProjects(rootProject, directlyChanged)

        // then
        result.size shouldBe 3
        result shouldContain ":common-lib"
        result shouldContain ":user-service"
        result shouldContain ":order-service"
    }

    test("should handle diamond dependency pattern") {
        // given
        val rootProject = ProjectBuilder.builder().build()

        // Build diamond pattern:
        //        api
        //       /   \
        //    impl1  impl2
        //       \   /
        //        app
        val api = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("api")
            .build()
        val impl1 = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("impl1")
            .build()
        val impl2 = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("impl2")
            .build()
        val app = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("app")
            .build()

        // Apply Java plugin to create configurations
        api.pluginManager.apply("java-library")
        impl1.pluginManager.apply("java-library")
        impl2.pluginManager.apply("java-library")
        app.pluginManager.apply("java-library")

        impl1.dependencies.add("implementation", api)
        impl2.dependencies.add("implementation", api)
        app.dependencies.add("implementation", impl1)
        app.dependencies.add("implementation", impl2)

        val logger = rootProject.logger
        val analyzer = DependencyAnalyzer(logger)
        val directlyChanged = setOf(":api")

        // when
        val result = analyzer.findAllAffectedProjects(rootProject, directlyChanged)

        // then
        result.size shouldBe 4
        result shouldContain ":api"
        result shouldContain ":impl1"
        result shouldContain ":impl2"
        result shouldContain ":app"
    }

    test("should handle multiple changed projects with overlapping dependency trees") {
        // given
        val rootProject = ProjectBuilder.builder().build()

        // Build overlapping trees:
        // service -> lib-a
        // service -> lib-b
        // lib-a and lib-b both change
        val libA = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("lib-a")
            .build()
        val libB = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("lib-b")
            .build()
        val service = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("service")
            .build()

        // Apply Java plugin to create configurations
        libA.pluginManager.apply("java-library")
        libB.pluginManager.apply("java-library")
        service.pluginManager.apply("java-library")

        service.dependencies.add("implementation", libA)
        service.dependencies.add("implementation", libB)

        val logger = rootProject.logger
        val analyzer = DependencyAnalyzer(logger)
        val directlyChanged = setOf(":lib-a", ":lib-b")

        // when
        val result = analyzer.findAllAffectedProjects(rootProject, directlyChanged)

        // then
        result.size shouldBe 3
        result shouldContain ":lib-a"
        result shouldContain ":lib-b"
        result shouldContain ":service"
    }

    test("should handle complex monorepo dependency graph") {
        // given
        val rootProject = ProjectBuilder.builder().build()

        // Build realistic monorepo structure:
        //          core-utils
        //         /     |     \
        //    user-api  order-api  payment-api
        //         \     |     /
        //          api-gateway
        val coreUtils = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("core-utils")
            .build()
        val userApi = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("user-api")
            .build()
        val orderApi = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("order-api")
            .build()
        val paymentApi = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("payment-api")
            .build()
        val apiGateway = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("api-gateway")
            .build()

        // Apply Java plugin to create configurations
        coreUtils.pluginManager.apply("java-library")
        userApi.pluginManager.apply("java-library")
        orderApi.pluginManager.apply("java-library")
        paymentApi.pluginManager.apply("java-library")
        apiGateway.pluginManager.apply("java-library")

        userApi.dependencies.add("implementation", coreUtils)
        orderApi.dependencies.add("implementation", coreUtils)
        paymentApi.dependencies.add("implementation", coreUtils)
        apiGateway.dependencies.add("implementation", userApi)
        apiGateway.dependencies.add("implementation", orderApi)
        apiGateway.dependencies.add("implementation", paymentApi)

        val logger = rootProject.logger
        val analyzer = DependencyAnalyzer(logger)
        val directlyChanged = setOf(":core-utils")

        // when
        val result = analyzer.findAllAffectedProjects(rootProject, directlyChanged)

        // then
        result.size shouldBe 5
        result shouldContain ":core-utils"
        result shouldContain ":user-api"
        result shouldContain ":order-api"
        result shouldContain ":payment-api"
        result shouldContain ":api-gateway"
    }

    test("should only include dependents of changed project not unrelated projects") {
        // given
        val rootProject = ProjectBuilder.builder().build()

        // Build two separate chains:
        // Chain 1: service-a -> lib-a
        // Chain 2: service-b -> lib-b (unrelated)
        val libA = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("lib-a")
            .build()
        val serviceA = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("service-a")
            .build()
        val libB = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("lib-b")
            .build()
        val serviceB = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("service-b")
            .build()

        // Apply Java plugin to create configurations
        libA.pluginManager.apply("java-library")
        serviceA.pluginManager.apply("java-library")
        libB.pluginManager.apply("java-library")
        serviceB.pluginManager.apply("java-library")

        serviceA.dependencies.add("implementation", libA)
        serviceB.dependencies.add("implementation", libB)

        val logger = rootProject.logger
        val analyzer = DependencyAnalyzer(logger)
        val directlyChanged = setOf(":lib-a")

        // when
        val result = analyzer.findAllAffectedProjects(rootProject, directlyChanged)

        // then
        result.size shouldBe 2
        result shouldContain ":lib-a"
        result shouldContain ":service-a"
        result shouldNotContain ":lib-b"
        result shouldNotContain ":service-b"
    }
})
