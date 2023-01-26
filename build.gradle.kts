import org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

val kotlinVersion = "1.8.0"
val serializationVersion = "1.4.1"
val kotlinxDateTimeVersion = "0.4.0"
val kotlinxCoroutinesTestVersion = "1.6.4"
val ktor_version = "2.2.1"
val logbackVersion = "1.4.5"
val reactVersion = "18.2.0-pre.479"
val kotlinWrappersVersion = "1.0.0-pre.484"
val testingLibraryReactVersion = "13.4.0"
val reactTestRendererVersion = "18.2.0"
val kotestVersion = "5.5.4"
val webDriverVersion = "5.3.1"
val awaitilityVersion = "4.2.0"
val cucumberVersion = "7.10.1"
val commonsIoVersion = "2.11.0"
val seleniumJavaVersion = "4.2.2"

plugins {
    kotlin("multiplatform") version "1.8.0"
    application
    kotlin("plugin.serialization") version "1.8.0"
    id("io.ktor.plugin") version "2.2.1"
}

group = "io.rippledown"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}
kotlin {
    jvm {
        compilations {
            val integrationTest by compilations.creating {
                defaultSourceSet {
                    dependencies {
                        implementation(kotlin("test"))
                        implementation(kotlin("test-common"))
                        implementation(kotlin("test-annotations-common"))
                        implementation(kotlin("test-junit"))
                        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
                        implementation("io.ktor:ktor-client-core:$ktor_version")
                        implementation("io.ktor:ktor-client-cio:$ktor_version")
                        implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
                        implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
                        implementation("io.ktor:ktor-client-cio:$ktor_version")
                        implementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
                        implementation("org.seleniumhq.selenium:selenium-java:$seleniumJavaVersion")
                        implementation("io.github.bonigarcia:webdrivermanager:$webDriverVersion")
                        implementation("io.kotest:kotest-assertions-core:$kotestVersion")
                        implementation("org.awaitility:awaitility-kotlin:$awaitilityVersion")
                    }
                    dependsOn(sourceSets.getByName("jvmMain"))
                }

                // Create a test task to run the tests produced by this compilation:
                tasks.register<Test>("integrationTest") {
                    dependsOn(tasks.shadowJar)
                    group = VERIFICATION_GROUP

                    // Run the tests with the classpath containing the compile dependencies (including 'main'),
                    // runtime dependencies, and the outputs of this compilation:
                    classpath = compileDependencyFiles + runtimeDependencyFiles + output.allOutputs

                    // Run only the tests from this compilation's outputs:
                    testClassesDirs = output.classesDirs
                }
            }

            val cucumberTest by compilations.creating {
                defaultSourceSet {
                    dependencies {
                        implementation("io.cucumber:cucumber-java8:$cucumberVersion")
                        implementation("io.cucumber:cucumber-junit:$cucumberVersion")
                        implementation("io.cucumber:cucumber-picocontainer:$cucumberVersion")
                    }
                    dependsOn(sourceSets.getByName("jvmMain"))
                    dependsOn(sourceSets.getByName("jvmIntegrationTest"))
                }
                val requirement = if (!project.hasProperty("requirement")) {
                    "single"
                } else {
                    project.property("requirement")
                }
                val pathToRequirements = "${projectDir.path}/src/jvmCucumberTest/resources/requirements"
                val argsForCuke = listOf(
                    "--plugin", "junit:build/test-results/junit.xml",
                    "--plugin", "html:build/test-results-html",
                    "--tags", "@$requirement",
                    "--glue", "steps",
                    pathToRequirements
                )

                tasks.create("cucumberTest") {
                    doLast {
                        javaexec {
                            maxHeapSize = "32G"
                            main = "io.cucumber.core.cli.Main"
                            args = argsForCuke
                            classpath = compileDependencyFiles + runtimeDependencyFiles + output.allOutputs
                        }
                    }
                    dependsOn(
                        tasks.shadowJar,
                        tasks.compileTestJava,
                        tasks.processTestResources,
                        tasks.getByName("jvmCucumberTestClasses")
                    )
                    group = VERIFICATION_GROUP
                }
            }
        }
        withJava()
    }

    js(IR) {
        browser {
            binaries.executable()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
                implementation("io.ktor:ktor-client-core:$ktor_version")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDateTimeVersion")
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
                implementation("commons-io:commons-io:$commonsIoVersion")
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
                implementation(kotlin("test"))
                implementation("org.seleniumhq.selenium:selenium-java:$seleniumJavaVersion")
                implementation("io.github.bonigarcia:webdrivermanager:$webDriverVersion")
                implementation("commons-io:commons-io:$commonsIoVersion")
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-js:$ktor_version")
                implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
                implementation(enforcedPlatform("org.jetbrains.kotlin-wrappers:kotlin-wrappers-bom:$kotlinWrappersVersion"))
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-emotion")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-mui")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-mui-icons")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(enforcedPlatform("org.jetbrains.kotlin-wrappers:kotlin-wrappers-bom:$kotlinWrappersVersion"))
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom-test-utils")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-extensions")

                implementation("io.ktor:ktor-client-mock:$ktor_version")

                implementation(kotlin("test-js"))
                implementation(npm("@testing-library/react", testingLibraryReactVersion))
                implementation(npm("react-test-renderer", reactTestRendererVersion))

                implementation("io.kotest:kotest-assertions-core-js:$kotestVersion")
                implementation("io.kotest:kotest-framework-api-js:$kotestVersion")
                implementation("io.kotest:kotest-framework-engine-js:$kotestVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesTestVersion")
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