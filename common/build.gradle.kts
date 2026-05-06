plugins {
    id("kotlin-library-conventions")
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

apply(from = "$rootDir/gradle/yguard.gradle")

// EXPERIMENT: expose the obfuscated jar (rather than the raw classes dir /
// unobfuscated jar) as :common's outgoing artifact so consumers like
// :server:shadowJar bundle the obfuscated bytecode. The `obfuscate` task
// (defined in gradle/yguard.gradle) overwrites build/libs/common.jar in
// place, so we point apiElements/runtimeElements at that same file but
// declare the obfuscate task as its producer.
val obfuscateTask = tasks.named("obfuscate")
val obfuscatedJar = tasks.jar.flatMap { it.archiveFile }

configurations.named("apiElements") {
    outgoing.artifacts.clear()
}
configurations.named("runtimeElements") {
    outgoing.artifacts.clear()
}
artifacts {
    add("apiElements", obfuscatedJar) { builtBy(obfuscateTask) }
    add("runtimeElements", obfuscatedJar) { builtBy(obfuscateTask) }
}