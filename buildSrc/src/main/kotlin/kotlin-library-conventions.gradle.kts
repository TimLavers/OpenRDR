plugins {
    id("repositories-conventions")
    kotlin("jvm")
    `java-library`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
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
