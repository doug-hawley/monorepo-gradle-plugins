plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.3.0"
}

group = "io.github.doug-hawley"
version = "1.0.0"

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
    website = "https://github.com/doug-hawley/monorepo-changed-projects-plugin"
    vcsUrl = "https://github.com/doug-hawley/monorepo-changed-projects-plugin.git"

    plugins {
        register("monorepoChangedProjectsPlugin") {
            id = "io.github.doug-hawley.monorepo-changed-projects-plugin"
            implementationClass = "io.github.doughawley.monorepochangedprojects.MonorepoChangedProjectsPlugin"
            displayName = "Monorepo Changed Projects Plugin"
            description = "A Gradle plugin to detect changed projects in a monorepo based on git history"
            tags = listOf("monorepo", "git", "ci", "optimization", "build")
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
