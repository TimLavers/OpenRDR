plugins {
    id("kotlin-library-conventions")
    kotlin("plugin.serialization")
    `java-test-fixtures`
    alias(libs.plugins.kover)
}
dependencies {
    implementation(libs.bundles.kotlinx)
    implementation(libs.logback)

    testImplementation(libs.bundles.ktor)
    testImplementation(libs.kotestAssertions)
    testFixturesImplementation(kotlin("test"))
    testFixturesImplementation(libs.kotlinxSerializationJson)
    testFixturesImplementation(libs.kotestAssertions)
    testFixturesImplementation(libs.bundles.ktor)

}
sourceSets {
    testFixtures {
        kotlin.srcDir("src/testFixtures/kotlin")
    }
}