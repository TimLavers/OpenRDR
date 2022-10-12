import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

val kotlinVersion = "1.7.10"
val serializationVersion = "1.4.0"
val ktor_version = "2.0.3"
val logbackVersion = "1.2.10"
val reactVersion = "17.0.2-pre.299-kotlin-1.6.10"

val kotlinExtensionsVersion = "1.0.1-pre.364"
val testingLibraryReactVersion = "13.3.0"
val reactTestRendererVersion = "17.0.2"
val kotestVersion = "5.4.1"
val webDriverVersion = "4.4.3"

plugins {
    kotlin("multiplatform") version "1.7.10"
    application
    kotlin("plugin.serialization") version "1.7.10"
    id("io.ktor.plugin") version "2.1.2"
}

group = "io.rippledown"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}
kotlin {
    jvm {
        withJava()
    }
    js {
        browser {
            binaries.executable()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
                implementation("io.ktor:ktor-client-core:$ktor_version")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
                implementation(kotlin("test"))
                implementation("io.kotest:kotest-assertions-core:$kotestVersion")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")

                implementation("io.ktor:ktor-serialization:$ktor_version")
                implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
                implementation("io.ktor:ktor-server-cors:$ktor_version")
                implementation("io.ktor:ktor-server-compression:$ktor_version")
                implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
                implementation("io.ktor:ktor-server-call-logging:$ktor_version")
                implementation("io.ktor:ktor-server-netty:$ktor_version")
                implementation("ch.qos.logback:logback-classic:$logbackVersion")
                implementation("commons-io:commons-io:2.11.0")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")

                implementation("io.ktor:ktor-client-core:$ktor_version")
                implementation("io.ktor:ktor-client-cio:$ktor_version")
                implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
                implementation("io.ktor:ktor-client-cio:$ktor_version")
//                implementation("io.ktor:ktor-server-test-host:$ktor_version")
                implementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
                implementation(kotlin("test"))
                implementation("org.seleniumhq.selenium:selenium-java:4.2.2")
                implementation("io.github.bonigarcia:webdrivermanager:$webDriverVersion")
                implementation("commons-io:commons-io:2.11.0")

            }
        }
        val jsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-js:$ktor_version")
                implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react:$reactVersion")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom:$reactVersion")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react-css:$reactVersion")

            }
        }
        val jsTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react-legacy:$reactVersion")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom-legacy:$reactVersion")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-extensions:$kotlinExtensionsVersion")
                implementation("io.ktor:ktor-client-mock:$ktor_version")

                implementation(kotlin("test-js"))
                implementation(npm("@testing-library/react", testingLibraryReactVersion))
                implementation(npm("react-test-renderer", reactTestRendererVersion))

                implementation("io.kotest:kotest-assertions-core-js:$kotestVersion")
                implementation("io.kotest:kotest-framework-api-js:$kotestVersion")
                implementation("io.kotest:kotest-framework-engine-js:$kotestVersion")

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")

            }
        }
    }
}

application {
    mainClass.set("OpenRDRServerKt")
}

tasks.getByName<Jar>("jvmJar") {
    includeJsArtifacts()
}

tasks.shadowJar {
    includeJsArtifacts()
}

// include JS artifacts in any JAR we generate
fun Jar.includeJsArtifacts() {
    val taskName = if (project.hasProperty("isProduction")
        || project.gradle.startParameter.taskNames.contains("installDist")
    ) {
        "jsBrowserProductionWebpack"
    } else {
        "jsBrowserDevelopmentWebpack"
    }
    val webpackTask = tasks.getByName<KotlinWebpack>(taskName)
    dependsOn(webpackTask) // make sure JS gets compiled first
    from(
        File(
            webpackTask.destinationDirectory,
            webpackTask.outputFileName
        )
    ) // bring output file along into the JAR

    manifest.attributes["Main-Class"] = "OpenRDRServerKt"
}

tasks {
    test {
        useJUnitPlatform()
    }
}

distributions {
    main {
        contents {
            from("$buildDir/libs") {
                rename("${rootProject.name}-jvm", rootProject.name)
                into("lib")
            }
        }
    }
}

// Alias "installDist" as "stage" (for cloud providers)
tasks.create("stage") {
    dependsOn(tasks.getByName("installDist"))
}

tasks.getByName<JavaExec>("run") {
    classpath(tasks.getByName<Jar>("jvmJar")) // so that the JS artifacts generated by `jvmJar` can be found and served
}