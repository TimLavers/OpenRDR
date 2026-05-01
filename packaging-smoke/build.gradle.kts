plugins {
    id("kotlin-library-conventions")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.kotestAssertions)
}

// The smoke test launches the actual zip distribution, so it needs the zip
// to exist. We do NOT auto-trigger :demoZip from the regular `test` task
// because that would slow every Gradle build; instead the dedicated
// `verifyDemoZip` task in the root build wires the dependency in.
tasks.test {
    // Forward the location of the demo zip + extraction root from Gradle to
    // the test JVM. The root build's verifyDemoZip task sets these.
    systemProperty("demoZip.path", System.getProperty("demoZip.path") ?: "")
    systemProperty(
        "demoZip.extractRoot",
        System.getProperty("demoZip.extractRoot")
            ?: layout.buildDirectory.dir("tmp/demo-zip-smoke").get().asFile.absolutePath
    )
    // Stream the test's stdout/stderr so progress is visible during the
    // long-running smoke (extract + JVM start + UI launch ~30-60s).
    testLogging {
        events("passed", "failed", "skipped", "standardOut", "standardError")
        showStandardStreams = true
    }
}
