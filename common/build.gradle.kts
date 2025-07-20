plugins {
    kotlin("plugin.serialization")
    `java-test-fixtures`
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