plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

group = "com.bitmoxie"
version = "1.0.0"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation("io.kotest:kotest-runner-junit5:6.1.3")
    testImplementation("io.kotest:kotest-assertions-core:6.1.3")
    testImplementation("io.kotest:kotest-property:6.1.3")
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
    add("unitTestImplementation", "io.kotest:kotest-runner-junit5:6.1.3")
    add("unitTestImplementation", "io.kotest:kotest-assertions-core:6.1.3")
    add("unitTestImplementation", "io.kotest:kotest-property:6.1.3")

    // Functional test dependencies
    add("functionalTestImplementation", gradleTestKit())
    add("functionalTestImplementation", "io.kotest:kotest-runner-junit5:6.1.3")
    add("functionalTestImplementation", "io.kotest:kotest-assertions-core:6.1.3")
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
    plugins {
        register("monorepoChangedProjectsPlugin") {
            id = "com.bitmoxie.monorepo-changed-projects"
            implementationClass = "com.bitmoxie.monorepochangedprojects.MonorepoChangedProjectsPlugin"
            displayName = "Monorepo Changed Projects Plugin"
            description = "A Gradle plugin to detect changed projects in a monorepo based on git history"
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
