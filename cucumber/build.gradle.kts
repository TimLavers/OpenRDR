import Version.awaitility
import Version.cucumber
import Version.kotest
import Version.ktor

apply(from = "../repositories.gradle.kts")

plugins {
    java
}

dependencies {
    testImplementation(testFixtures(project(":common")))

    testImplementation(project.dependencies.enforcedPlatform("io.cucumber:cucumber-bom:$cucumber"))
    testImplementation("io.cucumber:cucumber-java8")
    testImplementation("io.cucumber:cucumber-junit")
    testImplementation("io.cucumber:cucumber-picocontainer")
    testImplementation("io.kotest:kotest-assertions-core:$kotest")
    testImplementation("io.kotest:kotest-runner-junit5:$kotest")
    testImplementation(kotlin("test-junit"))
    testImplementation("org.awaitility:awaitility-kotlin:$awaitility")
    testImplementation(project.dependencies.enforcedPlatform("io.ktor:ktor-bom:$ktor"))
    testImplementation("io.ktor:ktor-client-core")
    testImplementation("io.ktor:ktor-client-cio")
    testImplementation("io.ktor:ktor-client-content-negotiation")
    testImplementation("io.ktor:ktor-serialization-kotlinx-json")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}


val pathToRequirements = "${projectDir.path}/src/test/resources/requirements"
val argsForCuke = mutableListOf(
    "--plugin", "junit:build/test-results/junit.xml",
    "--plugin", "html:build/test-results-html",
    "--glue", "steps",
    pathToRequirements
)

tasks.register<JavaExec>("cucumberTest") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    maxHeapSize = "32G"
    mainClass.set("io.cucumber.core.cli.Main")
//    classpath = compileDependencyFiles + runtimeDependencyFiles + output.allOutputs
    args = argsForCuke.apply {
        add("--tags")
        add("not @ignore")
    }
    dependsOn(
//        tasks.shadowJar,
        tasks.compileTestJava,
        tasks.processTestResources,
        tasks.getByName("testClasses")
    )
}

tasks.register<JavaExec>("cucumberSingleTest") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    maxHeapSize = "32G"
    mainClass.set("io.cucumber.core.cli.Main")
//    classpath = compileDependencyFiles + runtimeDependencyFiles + output.allOutputs
    args = argsForCuke.apply { add("--tags"); add("@single") }
    dependsOn(
//        tasks.shadowJar,
        tasks.compileTestJava,
        tasks.processTestResources,
        tasks.getByName("jvmCucumberTestClasses")
    )
}

tasks.register<Test>("integrationTest") {
//    dependsOn(tasks.shadowJar)
    group = LifecycleBasePlugin.VERIFICATION_GROUP

    // Run the tests with the classpath containing the compile dependencies (including 'main'),
    // runtime dependencies, and the outputs of this compilation:
//    classpath = compileDependencyFiles + runtimeDependencyFiles + output.allOutputs

    // Run only the tests from this compilation's outputs:
//    testClassesDirs = output.classesDirs
}

