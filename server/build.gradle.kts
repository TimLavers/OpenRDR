apply(from = "../repositories.gradle.kts")
plugins {
    id("io.ktor.plugin") version "2.3.5"
    kotlin("plugin.serialization")
    id("org.gretty") version "4.1.0"
}

dependencies {
    implementation(project(":common"))
    implementation(libs.bundles.ktor)
    implementation(libs.bundles.kotlinx)

    implementation("io.ktor:ktor-server-cors")
    implementation("io.ktor:ktor-server-compression")
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("io.ktor:ktor-server-netty")
    implementation(libs.logback)
    implementation(libs.commonsIo)
    implementation(libs.bundles.exposed)
    implementation("org.postgresql:postgresql:42.5.4")

    testImplementation(testFixtures(project(":common")))
    testImplementation(libs.kotestAssertions)
    testImplementation(libs.mockk)
    testImplementation(libs.commonsLang3)
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass.set("io.rippledown.server.OpenRDRServerKt")
}
