apply(from = "../repositories.gradle.kts")

plugins {
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":llm"))
    implementation(kotlin("reflect"))
    implementation(libs.logback)
    implementation(libs.generativeai)
    implementation(libs.kotlinxSerializationJson)
    implementation(libs.bundles.ktor)

    testImplementation(testFixtures(project(":common")))
    testImplementation(libs.kotlinxCoroutinesTest)
    testImplementation(libs.kotestAssertions)
    testImplementation(libs.mockk)
}