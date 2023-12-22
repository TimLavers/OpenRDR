
plugins {
    kotlin("plugin.serialization")
    id("java-test-fixtures")
}
dependencies {
    testFixturesImplementation(kotlin("test"))
    testFixturesImplementation("io.kotest:kotest-assertions-core:${Version.kotest}")

}
sourceSets {
    testFixtures{
        kotlin.srcDir("src/testFixtures/kotlin")
    }
}