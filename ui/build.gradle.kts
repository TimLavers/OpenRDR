import org.jetbrains.compose.desktop.application.dsl.TargetFormat

apply(from = "../repositories.gradle.kts")

plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":common"))
    implementation(compose.desktop.currentOs)
    implementation(compose.preview)
    implementation(compose.material3)
    implementation(libs.composePreviewDesktop)
    implementation(libs.logback)
    implementation("com.darkrockstudios:mpfilepicker:3.1.0")
    implementation(libs.bundles.ktor)
    implementation("io.ktor:ktor-client-logging:2.3.5")
    implementation(libs.bundles.kotlinx)

    testImplementation(compose.desktop.uiTestJUnit4)
    testImplementation(testFixtures(project(":common")))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation(libs.kotestAssertions)
    testImplementation(libs.mockk)
    testImplementation(libs.awaitility)
}

compose.desktop {
    application {
        mainClass = "Main"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi)
            packageName = "ui"
            packageVersion = "1.0.0"
        }
    }
}
