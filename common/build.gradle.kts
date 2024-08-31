plugins {
    kotlin("plugin.serialization")
    `java-test-fixtures`
}
dependencies {
    testFixturesImplementation(kotlin("test"))
    testFixturesImplementation(libs.kotlinxSerializationJson)
    testFixturesImplementation(libs.assertionsCore)

}
sourceSets {
    testFixtures {
        kotlin.srcDir("src/testFixtures/kotlin")
    }
}