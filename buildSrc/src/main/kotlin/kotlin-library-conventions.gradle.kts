plugins {
    id("repositories-conventions")
    kotlin("jvm")
    `java-library`
}

kotlin {
    jvmToolchain(21)
}

val libraries = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    // Keep the GenAI SDK's transitive Jackson modules on a patched, aligned release.
    implementation(platform(libraries.findLibrary("jacksonBom").get()))
    constraints {
        implementation(libraries.findLibrary("commonsCodec").get()) {
            because("Older HTTP client dependencies request vulnerable Commons Codec 1.11")
        }
        testImplementation(libraries.findLibrary("assertJCore").get()) {
            because("AssertJ Swing requests a version affected by CVE-2026-24400")
        }
    }
    testImplementation(kotlin("test"))
}

sourceSets {
    main {
        resources {
            srcDir(rootProject.projectDir.resolve("shared-resources"))
        }
    }
    test {
        resources {
            srcDir(rootProject.projectDir.resolve("shared-test-resources"))
        }
    }
}

tasks.test {
    if (project.name != "ui") {
        useJUnitPlatform()
    }
    jvmArgs("-Xshare:off", "-XX:+EnableDynamicAgentLoading")
}
