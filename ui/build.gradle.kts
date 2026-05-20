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
    implementation(project(":llm"))

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

            // The bundled runtime is also used to run the server fat jar
            // (see packaging/start-demo.{bat,sh}). Because the server jar is
            // not on the module path, jlink can't auto-detect its module
            // requirements, so we list them explicitly. On Windows we also
            // include jdk.crypto.mscapi, which supplies the WINDOWS-ROOT
            // keystore used when -Djavax.net.ssl.trustStoreType=WINDOWS-ROOT
            // is set to trust Windows-installed (e.g. corporate MITM) root CAs.
            val baseModules = listOf(
                "java.naming",
                "java.net.http",
                "java.sql",
                "java.management",
                "java.security.jgss",
                "jdk.crypto.cryptoki",
                "jdk.unsupported",
            )
            val platformModules = if (org.gradle.internal.os.OperatingSystem.current().isWindows)
                baseModules + "jdk.crypto.mscapi" else baseModules
            modules(*platformModules.toTypedArray())

            macOS {
                // Required for the chat-panel voice-input button. Without
                // NSMicrophoneUsageDescription, macOS TCC silently denies
                // mic access -- the user is never even prompted -- and
                // javax.sound.sampled.TargetDataLine reads zero bytes,
                // so Gemini transcription always returns empty.
                infoPlist {
                    extraKeysRawXml = """
                        <key>NSMicrophoneUsageDescription</key>
                        <string>OpenRDR uses the microphone to dictate chat messages.</string>
                    """.trimIndent()
                }
            }
        }
    }
}
compose.resources {
    generateResClass = always
}
