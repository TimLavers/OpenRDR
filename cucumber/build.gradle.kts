
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
    testImplementation(libs.composePreviewDesktop)
    testImplementation(libs.kotestAssertions)
    testImplementation(libs.assertJSwing)
    testImplementation(libs.logback)
    testImplementation(libs.guava)
    testImplementation(libs.awaitility)
    testImplementation(libs.bundles.ktor)
    testImplementation(libs.bundles.kotlinx)
    testImplementation(libs.bundles.cucumber)
    testImplementation(libs.junitPlatformSuite)
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.mockk)
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

val featureFolders = listOf(
    "attributes",
    "cases",
    "chat",
    "conditions",
    "interpreter",
    "kb",
    "rulebuilding",
    "samples"
)
fun runCucumber(cukeArgs: List<String>): Int {
    val javaHome = System.getProperty("java.home")
    val javaBin = "$javaHome/bin/java"
    val cp = cukeClassPath.asPath
    // Write JVM args to a temporary argfile to avoid Windows command-line length limits
    val argFile = File.createTempFile("cucumber-args", ".txt")
    argFile.deleteOnExit()
    argFile.writeText(
        listOf(
        "-Xmx4G",
        "--enable-native-access=ALL-UNNAMED",
        "-cp",
            "\"${cp.replace("\\", "\\\\")}\"",
        "io.cucumber.core.cli.Main"
        ).joinToString("\n")
    )
    val cmd = listOf(javaBin, "@${argFile.absolutePath}") + cukeArgs
    val process = ProcessBuilder(cmd)
        .directory(projectDir)
        .redirectErrorStream(true)
        .start()
    process.inputStream.bufferedReader().use { reader ->
        reader.lines().forEach { println(it) }
    }
    return process.waitFor()
}

featureFolders.forEach { folderName ->
    tasks.register(folderName) {
        group = "verification"
        dependsOn(prerequisiteTasks)
        doLast {
            val rerunFile = file("build/rerun_${folderName}.txt")
            if (rerunFile.exists()) rerunFile.delete()

            val cukeArgs = argsForCuke() + listOf(
                "--plugin", "rerun:${rerunFile.path}",
                "$pathToRequirements/$folderName",
                "--tags", "not @ignore"
            )
            val exitCode = runCucumber(cukeArgs)

            if (exitCode != 0 && rerunFile.exists() && rerunFile.readText().isNotBlank()) {
                println("\nRetrying failed scenarios for '$folderName':")
                println(rerunFile.readText())
                val rerunArgs = argsForCuke() + listOf("@${rerunFile.path}")
                val rerunExitCode = runCucumber(rerunArgs)
                if (rerunExitCode != 0) {
                    throw GradleException("Cucumber tests failed for $folderName (after retry)")
                }
            } else if (exitCode != 0) {
                throw GradleException("Cucumber tests failed for $folderName")
            }
        }
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
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    mainClass.set("io.cucumber.core.cli.Main")
    classpath = cukeClassPath
}