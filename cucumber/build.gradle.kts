
apply(from = "../repositories.gradle.kts")

plugins {
    java
    alias(libs.plugins.compose)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    testImplementation(project(":ui"))
    testImplementation(testFixtures(project(":common")))
    testImplementation(compose.desktop.currentOs)
    testImplementation(compose.preview)
    testImplementation(libs.composePreviewDesktop)
    testImplementation(libs.bundles.ktor)
    testImplementation(libs.logback)
    testImplementation(libs.bundles.kotlinx)
    testImplementation(libs.bundles.cucumber)
    testImplementation(libs.kotestAssertions)
    testImplementation(libs.assertJSwing)
    testImplementation(libs.bundles.ktor)
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.guava)
    testImplementation(libs.awaitility)
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

