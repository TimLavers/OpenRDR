import Version.assertjSwing
import Version.cucumber
import Version.guava
import Version.kotest
import Version.ktor

apply(from = "../repositories.gradle.kts")

plugins {
    java
    id("org.jetbrains.compose") version "1.6.11"
}

dependencies {
    testImplementation(project(":ui"))
    testImplementation(testFixtures(project(":common")))
    testImplementation(compose.desktop.currentOs)
    testImplementation(compose.preview)

    testImplementation("org.jetbrains.compose.ui:ui-tooling-preview-desktop:${Version.compose}")

    testImplementation(project.dependencies.enforcedPlatform("io.cucumber:cucumber-bom:$cucumber"))
    testImplementation("io.cucumber:cucumber-java")
    testImplementation("io.cucumber:cucumber-junit-platform-engine")

    testImplementation("io.cucumber:cucumber-junit")
    testImplementation("io.cucumber:cucumber-picocontainer")
    testImplementation("io.kotest:kotest-assertions-core:$kotest")
    testImplementation("org.assertj:assertj-swing:$assertjSwing")
    testImplementation(project.dependencies.enforcedPlatform("io.ktor:ktor-bom:$ktor"))
    testImplementation("io.ktor:ktor-client-core")
    testImplementation("io.ktor:ktor-client-cio")
    testImplementation("io.ktor:ktor-client-content-negotiation")
    testImplementation("io.ktor:ktor-serialization-kotlinx-json")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("com.google.guava:guava:$guava")
}

val cukeClassPath =
    configurations.testRuntimeClasspath.get()
        .plus(project.sourceSets.main.get().output)
        .plus(project.sourceSets.test.get().output)

val pathToRequirements = "${projectDir.path}/src/test/resources/requirements"
val argsForCuke = mutableListOf(
    "--plugin", "junit:build/test-results/junit.xml",
    "--plugin", "html:build/test-results-html",
    "--glue", "steps",
    pathToRequirements
)

tasks.register<JavaExec>("cucumberTest") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    maxHeapSize = "24G"
    mainClass.set("io.cucumber.core.cli.Main")
    classpath = cukeClassPath
    args = argsForCuke.apply {
        add("--tags")
        add("not @ignore")
    }
    dependsOn(
        ":server:shadowJar",
        tasks.compileTestJava,
        tasks.processTestResources,
        tasks.getByName("testClasses")
    )
}

tasks.register<JavaExec>("cucumberSingleTest") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    maxHeapSize = "32G"
    mainClass.set("io.cucumber.core.cli.Main")
    classpath = cukeClassPath
    args = argsForCuke.apply {
        add("--tags")
        add("@single")
    }
    dependsOn(
        ":server:shadowJar",
        tasks.compileTestJava,
        tasks.processTestResources,
        tasks.getByName("testClasses")
    )
}

