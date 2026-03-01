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

// Classes bundled into the plugin jar; not exposed as a POM dependency
val embed by configurations.creating {
    isTransitive = false
}

dependencies {
    implementation(kotlin("stdlib"))
    embed(project(":monorepo-plugin-core"))
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.kotest:kotest-property:5.9.1")
}

// Make embedded classes available for compilation, runtime, and all test source sets
// (compileClasspath/runtimeClasspath are not published in the POM)
configurations {
    compileClasspath { extendsFrom(embed) }
    runtimeClasspath { extendsFrom(embed) }
    testRuntimeClasspath { extendsFrom(embed) }
}

// Bundle embedded jar contents directly into the plugin jar
tasks.named<Jar>("jar") {
    dependsOn(embed)
    from(embed.map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
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
    add("unitTestImplementation", "io.kotest:kotest-framework-datatest:5.9.1")
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
        register("monorepoReleasePlugin") {
            id = "io.github.doug-hawley.monorepo-release-plugin"
            implementationClass = "io.github.doughawley.monoreporelease.MonorepoReleasePlugin"
            displayName = "Monorepo Release Plugin"
            description = "A Gradle plugin for per-project versioning and tagging in a Gradle monorepo"
            tags.set(listOf("monorepo", "git", "release", "versioning", "tagging"))
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
