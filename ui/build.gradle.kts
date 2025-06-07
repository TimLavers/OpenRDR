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
    implementation(compose.material)
    implementation(compose.material3)
    implementation(compose.components.resources)
    implementation(libs.composePreviewDesktop)
    implementation(libs.materialIcons)
    implementation(libs.logback)
    implementation(libs.filepicker)
    implementation(libs.bundles.ktor)
    implementation(libs.bundles.kotlinx)

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
        mainClass = "Main"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi)
            packageName = "ui"
            packageVersion = "1.0.0"
        }
    }
}
compose.resources {
    generateResClass = always
}
