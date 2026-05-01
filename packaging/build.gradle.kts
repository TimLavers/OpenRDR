plugins {
    id("kotlin-library-conventions")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.kotestAssertions)
}

// The smoke test launches the actual zip distribution, so we always make
// it depend on `:demoZip` producing the artefact. This means
// `./gradlew :packaging:test` (or `./gradlew clean :packaging:test`)
// just works end-to-end, and up-to-date checks ensure the zip is only
// rebuilt when inputs change.
val demoZipTask = rootProject.tasks.named<Zip>("demoZip")

tasks.test {
    dependsOn(demoZipTask)
    // Re-run whenever the produced zip changes so a fresh build is
    // exercised rather than a stale extract.
    inputs.file(demoZipTask.flatMap { it.archiveFile }).withPropertyName("demoZip")
    // The test has external side-effects (spawns processes, binds port
    // 9090) that Gradle's output checks can't see, so always re-run.
    outputs.upToDateWhen { false }

    doFirst {
        systemProperty("demoZip.path", demoZipTask.get().archiveFile.get().asFile.absolutePath)
    }
    systemProperty(
        "demoZip.extractRoot",
        layout.buildDirectory.dir("tmp/demo-zip-smoke").get().asFile.absolutePath
    )
    // Stream the test's stdout/stderr so progress is visible during the
    // long-running smoke (extract + JVM start + UI launch ~30-60s).
    testLogging {
        events("passed", "failed", "skipped", "standardOut", "standardError")
        showStandardStreams = true
    }
}
