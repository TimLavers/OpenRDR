import Version.mockk
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

apply(from = "repositories.gradle.kts")

plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    id("io.ktor.plugin") version "2.3.5"
}
kotlin {
    jvmToolchain(17)
}

subprojects {
    apply(from = "../repositories.gradle.kts")

    apply {
        plugin("kotlin")
        plugin("java-library")
    }

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-stdlib:${Version.kotlin}")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
        implementation(project.dependencies.enforcedPlatform("io.ktor:ktor-bom:${Version.ktor}"))
        implementation("io.ktor:ktor-client-core")
        implementation("io.ktor:ktor-client-cio")
        implementation("io.ktor:ktor-client-content-negotiation")
        implementation("io.ktor:ktor-serialization")
        implementation("io.ktor:ktor-serialization-kotlinx-json")
        implementation("org.jetbrains.kotlinx:kotlinx-datetime:${Version.kotlinxDateTimeVersion}")

        testImplementation(kotlin("test"))
        testImplementation("io.kotest:kotest-assertions-core:${Version.kotest}")
        testImplementation("io.ktor:ktor-client-mock")
        testImplementation("io.mockk:mockk:${mockk}")
    }
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = Version.java.toString()
        }
    }
    tasks.test {
        //compose desktop tests are not working with junit5, just use junit4 for now
        if ( project.name != "ui" ) {
            useJUnitPlatform()
        }
    }
}


group = "io.rippledown"
version = "1.0-SNAPSHOT"
