import org.gradle.jvm.toolchain.JavaLanguageVersion.of

apply(from = "repositories.gradle.kts")

plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("io.ktor.plugin") version "3.2.3"
    idea
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.versionCatalogUpdate)
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

kotlin {
    jvmToolchain(21)
}
java {
    toolchain {
        languageVersion = of(21)
    }
}

subprojects {
    apply(from = "../repositories.gradle.kts")

    apply {
        plugin("kotlin")
        plugin("java-library")
    }

    dependencies {
        testImplementation(kotlin("test"))
    }

    sourceSets {
        main {
            resources {
                srcDir(rootProject.projectDir.resolve("shared-resources"))
            }
        }
        test {
            resources {
                srcDir(rootProject.projectDir.resolve("shared-test-resources"))
            }
        }
    }

    tasks.test {
        if (project.name != "ui") {
            useJUnitPlatform()
        }
        jvmArgs("-Xshare:off", "-XX:+EnableDynamicAgentLoading")
    }
}

group = "io.rippledown"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("io.rippledown.server.OpenRDRServerKt")
}

tasks.register("runAllTests") {
    group = "verification"
    description = "Runs all unit tests followed by all Cucumber tests"
    val unitTests = subprojects.filter { it.name != "cucumber" }.map { it.tasks.named("test") }
    val cucumberTests = project(":cucumber").tasks.named("cucumberTest")
    dependsOn(unitTests)
    dependsOn(cucumberTests)
    cucumberTests.get().mustRunAfter(unitTests)
}