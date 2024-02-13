import org.jetbrains.compose.desktop.application.dsl.TargetFormat

apply(from = "../repositories.gradle.kts")

plugins {
    id("org.jetbrains.compose") version "1.5.12"
}

dependencies {
    implementation(project(":common"))
    implementation(compose.desktop.currentOs)
    implementation(compose.preview)
    implementation("org.jetbrains.compose.ui:ui-tooling-preview-desktop:${Version.compose}")

    testImplementation(compose.desktop.uiTestJUnit4)
    testImplementation(testFixtures(project(":common")))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
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
