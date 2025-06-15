apply(from = "../repositories.gradle.kts")

plugins {
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":common"))
    implementation(kotlin("reflect"))
    implementation(libs.logback)
    implementation(libs.generativeai)
    implementation(libs.ktorClientMock)
    implementation(libs.kotlinxSerializationJson)
    testImplementation(libs.kotestAssertions)
    testImplementation(libs.mockk)
}
