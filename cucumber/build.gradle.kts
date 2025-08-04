
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

val featureFolders = listOf("attributes", "cases", "conditions", "kb", "rulebuilding", "samples", "chat")
featureFolders.forEach { folderName ->
    tasks.register<JavaExec>(folderName) {
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
    dependsOn(featureFolders.map { folderName -> tasks.getByName(folderName) })
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

tasks.register<JavaExec>("cucumberFolderTest") {
    setupExec()
    val folder by extra {
        project.properties["folder"] ?: error("Folder must be specified using -Pfolder=<folderName>")
    }
    args = argsForCuke() + listOf(
        "$pathToRequirements/$folder",
        "--tags",
        "not @ignore"
    )
    dependsOn(prerequisiteTasks)
}

fun JavaExec.setupExec() {
    group = "verification"
    maxHeapSize = "4G"
    mainClass.set("io.cucumber.core.cli.Main")
    classpath = cukeClassPath
}