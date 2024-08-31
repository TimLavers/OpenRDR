import Version.awaitility
import Version.mockk
import org.gradle.jvm.toolchain.JavaLanguageVersion.of

apply(from = "repositories.gradle.kts")

plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
    id("io.ktor.plugin") version "2.3.5"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    idea
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.composeCompiler) apply false
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
        implementation("org.jetbrains.kotlin:kotlin-stdlib:${Version.kotlin}")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
        implementation(project.dependencies.enforcedPlatform("io.ktor:ktor-bom:${Version.ktor}"))
        implementation("io.ktor:ktor-client-core")
        implementation("io.ktor:ktor-client-cio")
        implementation("io.ktor:ktor-client-content-negotiation")
        implementation("io.ktor:ktor-serialization")
        implementation("io.ktor:ktor-serialization-kotlinx-json")

        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Version.kotlinxSerialization}")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${Version.kotlinxSerialization}")
        implementation("org.jetbrains.kotlinx:kotlinx-datetime:${Version.kotlinxDateTimeVersion}")

        testImplementation(kotlin("test"))
        testImplementation("io.kotest:kotest-assertions-core:${Version.kotest}")
        testImplementation("io.ktor:ktor-client-mock")
        testImplementation("io.mockk:mockk:${mockk}")
        testImplementation("org.awaitility:awaitility-kotlin:$awaitility")

    }

    tasks.test {
        //compose desktop tests are not working with junit5, just use junit4 for now
        if (project.name != "ui") {
            useJUnitPlatform()
        }
        jvmArgs("-Xshare:off")
    }
}

group = "io.rippledown"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("io.rippledown.server.OpenRDRServerKt")
}
