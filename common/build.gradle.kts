plugins {
    kotlin("plugin.serialization")
    `java-test-fixtures`
}
dependencies {
    testFixturesImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Version.kotlinxSerialization}")

    testFixturesImplementation(kotlin("test"))
    testFixturesImplementation("io.kotest:kotest-assertions-core:${Version.kotest}")

}
sourceSets {
    testFixtures {
        kotlin.srcDir("src/testFixtures/kotlin")
    }
}