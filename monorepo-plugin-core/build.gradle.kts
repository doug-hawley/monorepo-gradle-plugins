plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "io.github.doug-hawley"
version = "0.3.2" // x-release-please-version

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("stdlib"))
}

sourceSets {
    val unitTest by creating {
        kotlin {
            srcDir("src/test/unit/kotlin")
        }
        compileClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath.get()
        runtimeClasspath += output + compileClasspath
    }
}

dependencies {
    add("unitTestImplementation", "io.kotest:kotest-runner-junit5:5.9.1")
    add("unitTestImplementation", "io.kotest:kotest-assertions-core:5.9.1")
}

val unitTest by tasks.registering(Test::class) {
    description = "Runs unit tests"
    group = "verification"
    testClassesDirs = sourceSets["unitTest"].output.classesDirs
    classpath = sourceSets["unitTest"].runtimeClasspath
    useJUnitPlatform()
    outputs.upToDateWhen { false }
}

tasks.named("check") {
    dependsOn(unitTest)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    outputs.upToDateWhen { false }
}

publishing {
    repositories {
        maven {
            name = "local"
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}
