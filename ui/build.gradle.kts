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
    implementation("org.jetbrains.compose.ui:ui-tooling-preview-desktop:${Version.compose}")
    implementation("ch.qos.logback:logback-classic:${Version.logback}")
    implementation("com.darkrockstudios:mpfilepicker:3.1.0")

    testImplementation(compose.desktop.uiTestJUnit4)
    testImplementation(testFixtures(project(":common")))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

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
