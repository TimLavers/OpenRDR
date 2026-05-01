import org.gradle.jvm.toolchain.JavaLanguageVersion.of

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("io.ktor.plugin") version "3.2.3"
    id("repositories-conventions")
    idea
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.versionCatalogUpdate)
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

kotlin {
    jvmToolchain(21)
}
java {
    toolchain {
        languageVersion = of(21)
    }
}

group = "io.rippledown"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("io.rippledown.server.OpenRDRServerKt")
}

// ---------------------------------------------------------------------------
// Demo packaging
//
// `./gradlew demoZip` assembles a self-contained portable zip suitable for
// handing to a colleague to demo OpenRDR. It bundles:
//   - the server fat jar (shadowJar) run in in-memory mode, and
//   - the Compose Desktop UI distributable (with a bundled JRE), plus
//   - launch scripts and a README.
//
// The native UI launcher is OS-specific, so the resulting zip is for the OS
// that built it; the zip filename reflects this.
// ---------------------------------------------------------------------------
val demoOsClassifier: String = org.gradle.internal.os.OperatingSystem.current().let {
    when {
        it.isWindows -> "windows"
        it.isMacOsX -> "macos"
        it.isLinux -> "linux"
        else -> "unknown"
    }
}

tasks.register<Zip>("demoZip") {
    group = "distribution"
    description = "Builds a portable zip containing the OpenRDR server and UI for demo use."

    dependsOn(":server:shadowJar", ":ui:createDistributable")

    archiveBaseName.set("openrdr-demo")
    archiveClassifier.set(demoOsClassifier)
    archiveVersion.set(project.version.toString())
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    // Everything inside the zip lives under a single top-level folder so the
    // colleague can unzip wherever and get a clean layout.
    val topLevel = "openrdr-demo"

    // Server fat jar -> <top>/server/
    from(project(":server").tasks.named("shadowJar")) {
        into("$topLevel/server")
    }

    // UI distributable -> <top>/ui/
    //
    // createDistributable produces ui/build/compose/binaries/main/app/
    // containing the top-level launcher (e.g. OpenRDR.app on macOS,
    // OpenRDR/ with bin+lib on Windows/Linux). Copy as-is.
    val uiDistRoot = project(":ui").layout.buildDirectory
        .dir("compose/binaries/main/app")
    from(uiDistRoot) {
        into("$topLevel/ui")
        // Gradle's Zip task does not preserve source file permissions.
        // The .app launcher binary and any bin/ launchers (Linux/Windows)
        // must be executable when extracted.
        eachFile {
            val p = relativePath.pathString
            if (p.contains("/Contents/MacOS/") ||
                p.contains("/bin/") ||
                p.endsWith(".dylib") ||
                p.endsWith(".so")
            ) {
                permissions { unix("755") }
            }
        }
    }

    // Unix launcher needs +x when extracted on macOS/Linux.
    from(rootProject.file("packaging")) {
        into(topLevel)
        include("start-demo.sh")
        filePermissions { unix("755") }
    }
    // Everything else ships with regular file permissions.
    from(rootProject.file("packaging")) {
        into(topLevel)
        include("start-demo.bat", "README-demo.txt")
        filePermissions { unix("644") }
    }
}