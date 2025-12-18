import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("io.ktor.plugin") version "3.2.3"
    kotlin("plugin.serialization")
    id("org.gretty") version "4.1.0"
}

dependencies {
    implementation(project(":common"))
    implementation(project(":hints"))
    implementation(project(":chat"))

    implementation(libs.bundles.ktor)
    implementation(libs.bundles.kotlinx)
    implementation(libs.commonsIo)
    implementation(libs.bundles.exposed)
    implementation(libs.postgresql)
    implementation(libs.generativeai)
    implementation(libs.jte)

    testImplementation(testFixtures(project(":common")))
    testImplementation(libs.kotlinxCoroutinesTest)
    testImplementation(libs.kotestAssertions)
    testImplementation(libs.mockk)
    testImplementation(libs.commonsLang3)
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass.set("io.rippledown.server.OpenRDRServerKt")
}
tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("openrdr")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
    from("../shared-resources") //put logback.xml in the jar
}
