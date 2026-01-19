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
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")
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
