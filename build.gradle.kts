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
buildscript {
    dependencies {
//        classpath("com.google.android.libraries.mapsplatform.secrets-gradle-plugin:secrets-gradle-plugin:2.0.1")
    }
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
        testImplementation("io.ktor:ktor-client-mock")
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
