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

// On macOS the .app bundle ships ad-hoc signed by jpackage. The demo zip
// then injects an extra `java` binary into Contents/runtime/Contents/Home/bin/
// so the launch script can spin up the server without a system JDK. That
// extra file is NOT covered by jpackage's signature seal, so the resulting
// bundle fails `spctl` validation ("a sealed resource is missing or invalid").
// On macOS Tahoe (26+) TCC reacts to a broken seal by silently denying every
// permission request - including microphone - with no prompt and no entry in
// the privacy panel. Re-signing the modified bundle restores the seal and
// lets TCC prompt normally.
//
// Strategy: stage the .app + extra java binary into a build dir, codesign
// it there, then have demoZip pull the .app from the staging dir.
val javaHomeBin = file("${System.getProperty("java.home")}/bin")
val macUiAppStagingDir = layout.buildDirectory.dir("staging/demo-mac-app")

val stageMacUiApp = tasks.register<Sync>("stageMacUiApp") {
    onlyIf { demoOsClassifier == "macos" }
    dependsOn(":ui:createDistributable")

    // jpackage's bundled JRE marks files under `legal/**` as read-only (444),
    // which makes Sync's in-place overwrite fail with "Permission denied" on
    // re-runs. Wipe the staging dir first so we always copy into empty space.
    doFirst {
        delete(macUiAppStagingDir)
    }

    from(project(":ui").layout.buildDirectory.dir("compose/binaries/main/app/OpenRDR.app")) {
        into("OpenRDR.app")
    }
    from(javaHomeBin) {
        include("java")
        into("OpenRDR.app/Contents/runtime/Contents/Home/bin")
        filePermissions { unix("755") }
    }

    into(macUiAppStagingDir)
}

val signMacUiApp = tasks.register<Exec>("signMacUiApp") {
    onlyIf { demoOsClassifier == "macos" }
    dependsOn(stageMacUiApp)

    val appDir = macUiAppStagingDir.map { it.dir("OpenRDR.app").asFile }
    inputs.dir(appDir)
    outputs.dir(appDir)

    // Ad-hoc sign (no Apple Developer ID needed). --force overwrites the
    // existing jpackage signature; --deep re-signs nested binaries (the
    // bundled JRE, the libapplauncher, etc.) so the seal is consistent.
    commandLine("codesign", "--force", "--deep", "--sign", "-")
    argumentProviders.add(CommandLineArgumentProvider { listOf(appDir.get().absolutePath) })
}

tasks.register<Zip>("demoZip") {
    group = "distribution"
    description = "Builds a portable zip containing the OpenRDR server and UI for demo use."

    dependsOn(":server:shadowJar")
    if (demoOsClassifier == "macos") {
        dependsOn(signMacUiApp)
    } else {
        dependsOn(":ui:createDistributable")
    }

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
    // On macOS we use the staged + re-signed bundle (see stageMacUiApp /
    // signMacUiApp above). On other OSes we use the raw createDistributable
    // output directly: createDistributable produces
    // ui/build/compose/binaries/main/app/OpenRDR/ with bin+lib on
    // Windows/Linux.
    if (demoOsClassifier == "macos") {
        from(macUiAppStagingDir) {
            into("$topLevel/ui")
            eachFile {
                val p = relativePath.pathString
                if (p.contains("/Contents/MacOS/") ||
                    p.contains("/Contents/runtime/Contents/Home/bin/") ||
                    p.endsWith(".dylib")
                ) {
                    permissions { unix("755") }
                }
            }
        }
    } else {
        val uiDistRoot = project(":ui").layout.buildDirectory
            .dir("compose/binaries/main/app")
        from(uiDistRoot) {
            into("$topLevel/ui")
            // Gradle's Zip task does not preserve source file permissions.
            // The bin/ launchers must be executable when extracted.
            eachFile {
                val p = relativePath.pathString
                if (p.contains("/bin/") ||
                    p.endsWith(".so")
                ) {
                    permissions { unix("755") }
                }
            }
        }
    }

    // On Windows/Linux we still need to add the `java` launcher binary that
    // jpackage strips out (macOS handles this in stageMacUiApp above).
    when (demoOsClassifier) {
        "windows" -> from(javaHomeBin) {
            include("java.exe", "javaw.exe")
            into("$topLevel/ui/OpenRDR/runtime/bin")
        }

        "linux" -> from(javaHomeBin) {
            include("java")
            into("$topLevel/ui/OpenRDR/runtime/bin")
            filePermissions { unix("755") }
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

// ---------------------------------------------------------------------------
// `./gradlew verifyDemoZip` is an alias for the packaging smoke test. The
// :packaging:test task itself already depends on :demoZip and wires in
// the produced artefact (see packaging/build.gradle.kts), so this is
// just a convenience entry point. Kept OUT of the default `check`
// aggregate because the smoke takes 30-60 s and binds port 9090.
// ---------------------------------------------------------------------------
tasks.register("verifyDemoZip") {
    group = "verification"
    description = "Builds the demo zip and runs the packaging smoke test against it."
    dependsOn(":packaging:test")
}