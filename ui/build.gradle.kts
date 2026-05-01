import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("kotlin-library-conventions")
    alias(libs.plugins.compose)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":common"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material)
    implementation(compose.material3)
    implementation(compose.components.resources)
    implementation(libs.composePreviewDesktop)
    implementation(libs.materialIcons)
    implementation(libs.logback)
    implementation(libs.filepicker)
    implementation(libs.bundles.ktor)
    implementation(libs.bundles.kotlinx)
    implementation(libs.commonsCodec)
    implementation(libs.vosk)

    testImplementation(testFixtures(project(":common")))
    testImplementation(compose.desktop.uiTestJUnit4)
    testImplementation(libs.kotlinxCoroutinesTest)
    testImplementation(libs.kotestAssertions)
    testImplementation(libs.mockk)
    testImplementation(libs.awaitility)
    testImplementation(libs.assertJSwing)
}

compose.desktop {
    application {
        mainClass = "io.rippledown.main.MainKt"
        jvmArgs("--enable-native-access=ALL-UNNAMED")

        // Use the JDK Gradle is running on (must be >= the project's bytecode
        // target) for the bundled runtime that ships with createDistributable.
        // Without this, Compose Desktop 1.10.3 jlinks its default JetBrains
        // Runtime (Java 20), which fails to load classes compiled to
        // bytecode 65 (Java 21) with UnsupportedClassVersionError.
        javaHome = System.getProperty("java.home")

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi)
            packageName = "OpenRDR"
            packageVersion = "1.0.0"
        }
    }
}
compose.resources {
    generateResClass = always
}
