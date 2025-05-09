
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
    testImplementation(libs.kotestAssertions)
    testImplementation(libs.assertJSwing)
    testImplementation(libs.logback)
    testImplementation(libs.guava)
    testImplementation(libs.awaitility)
    testImplementation(libs.bundles.ktor)
    testImplementation(libs.bundles.kotlinx)
    testImplementation(libs.bundles.cucumber)
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

val cukeClassPath = configurations.testRuntimeClasspath.get()
    .plus(sourceSets.main.get().output)
    .plus(sourceSets.test.get().output)

val pathToRequirements = "${projectDir.path}/src/test/resources/requirements"
fun argsForCuke() = mutableListOf(
    "--plugin", "junit:build/test-results/junit.xml",
    "--plugin", "html:build/test-results-html",
    "--glue", "steps"
)

val prerequisiteTasks = listOf(
    ":server:shadowJar",
    tasks.compileTestJava,
    tasks.processTestResources,
    tasks.getByName("testClasses")
)

listOf("application", "attributes", "cases", "conditions", "kb", "rulebuilding", "samples").forEach { taskName ->
    tasks.register<JavaExec>(taskName) {
        setupExec()
        args = argsForCuke() + listOf(
            "$pathToRequirements/$name",
            "--tags",
            "not @ignore"
        )
        dependsOn(prerequisiteTasks)
    }
}

tasks.register("cucumberTest") {
    dependsOn(
        "attributes",
        "cases",
        "conditions",
        "kb",
        "rulebuilding",
        "samples"
    )
}

tasks.register<JavaExec>("cucumberSingleTest") {
    setupExec()
    args = argsForCuke() + listOf(
        pathToRequirements,
        "--tags",
        "@single"
    )
    dependsOn(prerequisiteTasks)
}

fun JavaExec.setupExec() {
    group = "verification"
    maxHeapSize = "4G"
    mainClass.set("io.cucumber.core.cli.Main")
    classpath = cukeClassPath
}