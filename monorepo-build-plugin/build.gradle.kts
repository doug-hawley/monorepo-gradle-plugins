plugins {
   `kotlin-dsl`
   `java-gradle-plugin`
   `maven-publish`
   id("com.gradle.plugin-publish") version "2.0.0"
}

group = "io.github.doug-hawley"
version = "0.3.2" // x-release-please-version

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.kotest:kotest-property:5.9.1")
}

// Configure source sets for unit and functional tests
sourceSets {
    val unitTest by creating {
        kotlin {
            srcDir("src/test/unit/kotlin")
        }
        resources {
            srcDir("src/test/unit/resources")
        }
        compileClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath.get()
        runtimeClasspath += output + compileClasspath
    }

    val functionalTest by creating {
        kotlin {
            srcDir("src/test/functional/kotlin")
        }
        resources {
            srcDir("src/test/functional/resources")
        }
        compileClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath.get()
        runtimeClasspath += output + compileClasspath
    }
}

// Add dependencies for the test source sets
dependencies {
    // Unit test dependencies
    add("unitTestImplementation", "io.kotest:kotest-runner-junit5:5.9.1")
    add("unitTestImplementation", "io.kotest:kotest-assertions-core:5.9.1")
    add("unitTestImplementation", "io.kotest:kotest-property:5.9.1")
    add("unitTestImplementation", "io.mockk:mockk:1.13.12")

    // Functional test dependencies
    add("functionalTestImplementation", gradleTestKit())
    add("functionalTestImplementation", "io.kotest:kotest-runner-junit5:5.9.1")
    add("functionalTestImplementation", "io.kotest:kotest-assertions-core:5.9.1")
}

// Register unit test task
val unitTest by tasks.registering(Test::class) {
    description = "Runs unit tests"
    group = "verification"
    testClassesDirs = sourceSets["unitTest"].output.classesDirs
    classpath = sourceSets["unitTest"].runtimeClasspath
    useJUnitPlatform()
    outputs.upToDateWhen { false }
}

// Register functional test task
val functionalTest by tasks.registering(Test::class) {
    description = "Runs functional tests"
    group = "verification"
    testClassesDirs = sourceSets["functionalTest"].output.classesDirs
    classpath = sourceSets["functionalTest"].runtimeClasspath
    useJUnitPlatform()
    outputs.upToDateWhen { false }
    shouldRunAfter(unitTest)
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}

// Make check depend on both test types
tasks.named("check") {
    dependsOn(unitTest, functionalTest)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    outputs.upToDateWhen { false }
}

gradlePlugin {
    website.set("https://github.com/doug-hawley/monorepo-gradle-plugins")
    vcsUrl.set("https://github.com/doug-hawley/monorepo-gradle-plugins.git")

    plugins {
        register("monorepoBuildPlugin") {
            id = "io.github.doug-hawley.monorepo-build-plugin"
            implementationClass = "io.github.doughawley.monorepobuild.MonorepoBuildPlugin"
            displayName = "Monorepo Build Plugin"
            description = "A Gradle plugin to selectively build changed projects in a monorepo based on git history"
            tags.set(listOf("monorepo", "git", "ci", "optimization", "build"))
        }
    }
}

publishing {
    repositories {
        maven {
            name = "local"
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}
