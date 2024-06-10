import Version.commonsIo
import Version.commonsLang3
import Version.diffUtils
import Version.exposed
import Version.logback

apply(from = "../repositories.gradle.kts")
plugins {
    id("io.ktor.plugin") version "2.3.5"
    kotlin("plugin.serialization") version "1.9.23"
    id("org.gretty") version "4.1.0"
}

dependencies {
    implementation(project(":common"))
    implementation(project.dependencies.enforcedPlatform("io.ktor:ktor-bom:${Version.ktor}"))
    implementation("io.ktor:ktor-serialization")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-cors")
    implementation("io.ktor:ktor-server-compression")
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("io.ktor:ktor-server-netty")
    implementation("ch.qos.logback:logback-classic:$logback")
    implementation("commons-io:commons-io:$commonsIo")
    implementation("org.jetbrains.exposed:exposed-core:$exposed")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed")
    implementation("org.postgresql:postgresql:42.5.4")
    implementation("io.github.java-diff-utils:java-diff-utils:$diffUtils")

    testImplementation(testFixtures(project(":common")))
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("io.ktor:ktor-server-test-host")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.apache.commons:commons-lang3:$commonsLang3")
}

application {
    mainClass.set("io.rippledown.server.OpenRDRServerKt")
}
