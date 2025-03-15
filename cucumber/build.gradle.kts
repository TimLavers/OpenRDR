
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
tasks.register<JavaExec>("application") {
    runCukesInDirectory(this@Build_gradle, this)
}
tasks.register<JavaExec>("attributes") {
    runCukesInDirectory(this@Build_gradle, this)
}
tasks.register<JavaExec>("cases") {
    runCukesInDirectory(this@Build_gradle, this)
}
tasks.register<JavaExec>("conditions") {
    runCukesInDirectory(this@Build_gradle, this)
}
tasks.register<JavaExec>("kb") {
    runCukesInDirectory(this@Build_gradle, this)
}
tasks.register<JavaExec>("rulebuilding") {
    runCukesInDirectory(this@Build_gradle, this)
}
tasks.register<JavaExec>("samples") {
    runCukesInDirectory(this@Build_gradle, this)
}
tasks.register("cucumberTest") {
    dependsOn(listOf(
        "attributes",
        "cases",
        "conditions",
        "kb",
        "rulebuilding",
        "samples",
    ))
}

tasks.register<JavaExec>("cucumberSingleTest") {
    setupExec(this, this@Build_gradle)
    args = argsForCuke().apply {
        add(pathToRequirements)
        add("--tags")
        add("@single")
    }
    dependsOn(prerequisiteTasks)
}

fun setupExec(javaExec: JavaExec, buildGradle: Build_gradle) {
    javaExec.group = LifecycleBasePlugin.VERIFICATION_GROUP
    javaExec.maxHeapSize = "4G"
    javaExec.mainClass.set("io.cucumber.core.cli.Main")
    javaExec.classpath = buildGradle.cukeClassPath
}

fun runCukesInDirectory(buildGradle: Build_gradle, javaExec: JavaExec) {
    buildGradle.setupExec(javaExec, buildGradle)
    javaExec.args = buildGradle.argsForCuke().apply {
        add("$pathToRequirements/${javaExec.name}")
        add("--tags")
        add("not @ignore")
    }
    javaExec.dependsOn(buildGradle.prerequisiteTasks)
}