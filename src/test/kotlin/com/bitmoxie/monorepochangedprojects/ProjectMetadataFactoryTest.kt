package com.bitmoxie.monorepochangedprojects

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.gradle.testfixtures.ProjectBuilder

class ProjectMetadataFactoryTest : FunSpec({

    test("should build metadata for root project with no dependencies") {
        // given
        val rootProject = ProjectBuilder.builder().build()
        val logger = rootProject.logger
        val factory = ProjectMetadataFactory(logger)

        // when
        val metadataMap = factory.buildProjectMetadataMap(rootProject)

        // then
        metadataMap.size shouldBe 1
        val rootMetadata = metadataMap[":"]
        rootMetadata shouldNotBe null
        rootMetadata!!.name shouldBe rootProject.name
        rootMetadata.fullyQualifiedName shouldBe ":"
        rootMetadata.dependencies shouldHaveSize 0
    }

    test("should build metadata for project with single subproject") {
        // given
        val rootProject = ProjectBuilder.builder().build()
        ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("submodule")
            .build()
        val logger = rootProject.logger
        val factory = ProjectMetadataFactory(logger)

        // when
        val metadataMap = factory.buildProjectMetadataMap(rootProject)

        // then
        metadataMap.size shouldBe 2

        val rootMetadata = metadataMap[":"]
        rootMetadata shouldNotBe null
        rootMetadata!!.name shouldBe rootProject.name
        rootMetadata.fullyQualifiedName shouldBe ":"

        val subMetadata = metadataMap[":submodule"]
        subMetadata shouldNotBe null
        subMetadata!!.name shouldBe "submodule"
        subMetadata.fullyQualifiedName shouldBe ":submodule"
    }

    test("should build metadata with simple dependency relationship") {
        // given
        val rootProject = ProjectBuilder.builder().build()
        val commonLib = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("common-lib")
            .build()
        val service = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("service")
            .build()

        commonLib.pluginManager.apply("java-library")
        service.pluginManager.apply("java-library")
        service.dependencies.add("implementation", commonLib)

        val logger = rootProject.logger
        val factory = ProjectMetadataFactory(logger)

        // when
        val metadataMap = factory.buildProjectMetadataMap(rootProject)

        // then
        metadataMap.size shouldBe 3

        val serviceMetadata = metadataMap[":service"]
        serviceMetadata shouldNotBe null
        serviceMetadata!!.dependencies shouldHaveSize 1
        serviceMetadata.dependencies[0].name shouldBe "common-lib"
        serviceMetadata.dependencies[0].fullyQualifiedName shouldBe ":common-lib"
    }

    test("should build metadata with transitive dependencies") {
        // given
        val rootProject = ProjectBuilder.builder().build()
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

        commonLib.pluginManager.apply("java-library")
        userService.pluginManager.apply("java-library")
        adminUi.pluginManager.apply("java-library")

        userService.dependencies.add("implementation", commonLib)
        adminUi.dependencies.add("implementation", userService)

        val logger = rootProject.logger
        val factory = ProjectMetadataFactory(logger)

        // when
        val metadataMap = factory.buildProjectMetadataMap(rootProject)

        // then
        metadataMap.size shouldBe 4

        val userServiceMetadata = metadataMap[":user-service"]
        userServiceMetadata shouldNotBe null
        userServiceMetadata!!.dependencies shouldHaveSize 1
        userServiceMetadata.dependencies[0].fullyQualifiedName shouldBe ":common-lib"

        val adminUiMetadata = metadataMap[":admin-ui"]
        adminUiMetadata shouldNotBe null
        adminUiMetadata!!.dependencies shouldHaveSize 1
        adminUiMetadata.dependencies[0].fullyQualifiedName shouldBe ":user-service"
    }

    test("should build metadata with multiple dependencies") {
        // given
        val rootProject = ProjectBuilder.builder().build()
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
        val apiGateway = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("api-gateway")
            .build()

        coreUtils.pluginManager.apply("java-library")
        userApi.pluginManager.apply("java-library")
        orderApi.pluginManager.apply("java-library")
        apiGateway.pluginManager.apply("java-library")

        userApi.dependencies.add("implementation", coreUtils)
        orderApi.dependencies.add("implementation", coreUtils)
        apiGateway.dependencies.add("implementation", userApi)
        apiGateway.dependencies.add("implementation", orderApi)

        val logger = rootProject.logger
        val factory = ProjectMetadataFactory(logger)

        // when
        val metadataMap = factory.buildProjectMetadataMap(rootProject)

        // then
        metadataMap.size shouldBe 5

        val apiGatewayMetadata = metadataMap[":api-gateway"]
        apiGatewayMetadata shouldNotBe null
        apiGatewayMetadata!!.dependencies shouldHaveSize 2

        val dependencyNames = apiGatewayMetadata.dependencies.map { it.name }
        dependencyNames shouldContain "user-api"
        dependencyNames shouldContain "order-api"
    }

    test("should build single project metadata") {
        // given
        val rootProject = ProjectBuilder.builder().build()
        val commonLib = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("common-lib")
            .build()
        val service = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("service")
            .build()

        commonLib.pluginManager.apply("java-library")
        service.pluginManager.apply("java-library")
        service.dependencies.add("implementation", commonLib)

        val logger = rootProject.logger
        val factory = ProjectMetadataFactory(logger)

        // when
        val serviceMetadata = factory.buildProjectMetadata(service)

        // then
        serviceMetadata.name shouldBe "service"
        serviceMetadata.fullyQualifiedName shouldBe ":service"
        serviceMetadata.dependencies shouldHaveSize 1
        serviceMetadata.dependencies[0].name shouldBe "common-lib"
    }

    test("should handle project with no dependencies") {
        // given
        val rootProject = ProjectBuilder.builder().build()
        val standalone = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("standalone")
            .build()

        standalone.pluginManager.apply("java-library")

        val logger = rootProject.logger
        val factory = ProjectMetadataFactory(logger)

        // when
        val metadataMap = factory.buildProjectMetadataMap(rootProject)

        // then
        val standaloneMetadata = metadataMap[":standalone"]
        standaloneMetadata shouldNotBe null
        standaloneMetadata!!.dependencies shouldHaveSize 0
    }

    test("should use findDependencyByName to locate dependency") {
        // given
        val rootProject = ProjectBuilder.builder().build()
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

        libA.pluginManager.apply("java-library")
        libB.pluginManager.apply("java-library")
        service.pluginManager.apply("java-library")

        service.dependencies.add("implementation", libA)
        service.dependencies.add("implementation", libB)

        val logger = rootProject.logger
        val factory = ProjectMetadataFactory(logger)

        // when
        val metadataMap = factory.buildProjectMetadataMap(rootProject)
        val serviceMetadata = metadataMap[":service"]

        // then
        serviceMetadata shouldNotBe null
        val foundLibA = serviceMetadata!!.findDependencyByName("lib-a")
        foundLibA shouldNotBe null
        foundLibA!!.fullyQualifiedName shouldBe ":lib-a"

        val foundLibB = serviceMetadata.findDependencyByName("lib-b")
        foundLibB shouldNotBe null
        foundLibB!!.fullyQualifiedName shouldBe ":lib-b"
    }

    test("should use findDependencyRecursively to locate transitive dependency") {
        // given
        val rootProject = ProjectBuilder.builder().build()
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

        commonLib.pluginManager.apply("java-library")
        userService.pluginManager.apply("java-library")
        adminUi.pluginManager.apply("java-library")

        userService.dependencies.add("implementation", commonLib)
        adminUi.dependencies.add("implementation", userService)

        val logger = rootProject.logger
        val factory = ProjectMetadataFactory(logger)

        // when
        val metadataMap = factory.buildProjectMetadataMap(rootProject)
        val adminUiMetadata = metadataMap[":admin-ui"]

        // then
        adminUiMetadata shouldNotBe null
        val foundCommonLib = adminUiMetadata!!.findDependencyRecursively("common-lib")
        foundCommonLib shouldNotBe null
        foundCommonLib!!.fullyQualifiedName shouldBe ":common-lib"

        val foundUserService = adminUiMetadata.findDependencyRecursively("user-service")
        foundUserService shouldNotBe null
        foundUserService!!.fullyQualifiedName shouldBe ":user-service"
    }
})
