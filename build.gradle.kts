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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
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
