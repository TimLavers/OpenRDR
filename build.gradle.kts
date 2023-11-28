import org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

val kotlinWrappersVersion = "1.0.0-pre.633"
val treeViewVersion = "6.0.0-beta.0-pre.633"
val kotlinxCoroutinesTestVersion = "1.7.3"
val kotlinxDateTimeVersion = "0.4.0"
val ktorVersion = "2.3.5"
val kotestVersion = "5.7.2"
val exposedVersion = "0.42.0"
val logbackVersion = "1.4.11"
val diffUtilsVersion = "4.12"
val testingLibraryReactVersion = "14.0.0"
val webDriverVersion = "5.5.3"
val seleniumJavaVersion = "4.14.0"
val awaitilityVersion = "4.2.0"
val cucumberVersion = "7.14.0"
val commonsIoVersion = "2.14.0"
val commonsTextVersion = "1.10.0"
val mockkVersion = "1.13.8"
val testNgVersion = "7.8.0"

plugins {
    kotlin("multiplatform") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20"
    id("io.ktor.plugin") version "2.3.5"
    id("org.gretty") version "4.1.0"
    application
}


group = "io.rippledown"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}
dependencies {
    implementation("org.testng:testng:$testNgVersion")
}

kotlin {
    jvmToolchain(17)

    js(IR) {
        browser {
            binaries.executable()
        }
    }
    jvm {
        withJava()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")
                implementation(project.dependencies.enforcedPlatform("io.ktor:ktor-bom:$ktorVersion"))
                implementation("io.ktor:ktor-client-core")
                implementation("io.ktor:ktor-serialization-kotlinx-json")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDateTimeVersion")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("io.kotest:kotest-assertions-core:$kotestVersion")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(project.dependencies.enforcedPlatform("io.ktor:ktor-bom:$ktorVersion"))
                implementation("io.ktor:ktor-serialization")
                implementation("io.ktor:ktor-server-content-negotiation")
                implementation("io.ktor:ktor-serialization-kotlinx-json")
                implementation("io.ktor:ktor-server-cors")
                implementation("io.ktor:ktor-server-compression")
                implementation("io.ktor:ktor-server-core-jvm")
                implementation("io.ktor:ktor-server-call-logging")
                implementation("io.ktor:ktor-server-netty")
                implementation("ch.qos.logback:logback-classic:$logbackVersion")
                implementation("commons-io:commons-io:$commonsIoVersion")
                implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
                implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
                implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
                implementation("org.postgresql:postgresql:42.5.4")
                implementation("io.github.java-diff-utils:java-diff-utils:$diffUtilsVersion")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(project.dependencies.enforcedPlatform("io.ktor:ktor-bom:$ktorVersion"))
                implementation("io.ktor:ktor-serialization-kotlinx-json")
                implementation("io.ktor:ktor-client-core:")
                implementation("io.ktor:ktor-client-cio")
                implementation("io.ktor:ktor-client-content-negotiation")
                implementation("io.ktor:ktor-client-cio")
                implementation("io.ktor:ktor-server-test-host")
                implementation("org.seleniumhq.selenium:selenium-java:$seleniumJavaVersion")

                implementation("io.github.bonigarcia:webdrivermanager:$webDriverVersion")
                implementation("commons-io:commons-io:$commonsIoVersion")
                implementation("io.mockk:mockk:${mockkVersion}")
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(project.dependencies.enforcedPlatform("io.ktor:ktor-bom:$ktorVersion"))
                implementation("io.ktor:ktor-client-js")
                implementation("io.ktor:ktor-client-content-negotiation")
                implementation("io.ktor:ktor-serialization-kotlinx-json")
                implementation(project.dependencies.enforcedPlatform("org.jetbrains.kotlin-wrappers:kotlin-wrappers-bom:$kotlinWrappersVersion"))
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-emotion")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-mui")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-mui-lab")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-mui-icons")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-muix-tree-view:$treeViewVersion")

                implementation(npm("debounce", "1.2.1"))
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(project.dependencies.enforcedPlatform("org.jetbrains.kotlin-wrappers:kotlin-wrappers-bom:$kotlinWrappersVersion"))
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom-test-utils")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-extensions")
                implementation("io.ktor:ktor-client-mock:$ktorVersion")

                implementation(kotlin("test-js"))
                implementation(npm("@testing-library/react", testingLibraryReactVersion))

                implementation("io.kotest:kotest-assertions-core-js:$kotestVersion")
                implementation("io.kotest:kotest-framework-api-js:$kotestVersion")
                implementation("io.kotest:kotest-framework-engine-js:$kotestVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesTestVersion")
            }
        }
    }

    jvm {

        compilations {
            val cucumberTest by compilations.creating {
                defaultSourceSet {
                    dependsOn(sourceSets.getByName("commonMain"))
                    dependencies {
                        implementation(project.dependencies.enforcedPlatform("io.cucumber:cucumber-bom:$cucumberVersion"))
                        implementation("io.cucumber:cucumber-java8")
                        implementation("io.cucumber:cucumber-junit")
                        implementation("io.cucumber:cucumber-picocontainer")
                        implementation("io.kotest:kotest-assertions-core:$kotestVersion")
                        implementation("io.kotest:kotest-runner-junit5:$kotestVersion")
                        implementation(kotlin("test-junit"))
                        implementation("org.awaitility:awaitility-kotlin:$awaitilityVersion")
                        implementation("org.seleniumhq.selenium:selenium-java:$seleniumJavaVersion")
                        implementation("io.github.bonigarcia:webdrivermanager:$webDriverVersion")

                        implementation(project.dependencies.enforcedPlatform("io.ktor:ktor-bom:$ktorVersion"))
                        implementation("io.ktor:ktor-client-core")
                        implementation("io.ktor:ktor-client-cio")
                        implementation("io.ktor:ktor-client-content-negotiation")
                        implementation("io.ktor:ktor-serialization-kotlinx-json")
                    }
                }
                val pathToRequirements = "${projectDir.path}/src/jvmCucumberTest/resources/requirements"
                val argsForCuke = mutableListOf(
                    "--plugin", "junit:build/test-results/junit.xml",
                    "--plugin", "html:build/test-results-html",
                    "--glue", "steps",
                    pathToRequirements
                )

                tasks.register<JavaExec>("cucumberTest") {
                    group = VERIFICATION_GROUP
                    maxHeapSize = "32G"
                    mainClass.set("io.cucumber.core.cli.Main")
                    classpath = compileDependencyFiles + runtimeDependencyFiles + output.allOutputs
                    args = argsForCuke.apply { add("--tags"); add("not @ignore") }
                    dependsOn(
                        tasks.shadowJar,
                        tasks.compileTestJava,
                        tasks.processTestResources,
                        tasks.getByName("jvmCucumberTestClasses")
                    )
                }

                tasks.register<JavaExec>("cucumberSingleTest") {
                    group = VERIFICATION_GROUP
                    maxHeapSize = "32G"
                    mainClass.set("io.cucumber.core.cli.Main")
                    classpath = compileDependencyFiles + runtimeDependencyFiles + output.allOutputs
                    args = argsForCuke.apply { add("--tags"); add("@single") }
                    dependsOn(
                        tasks.shadowJar,
                        tasks.compileTestJava,
                        tasks.processTestResources,
                        tasks.getByName("jvmCucumberTestClasses")
                    )
                }

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
        }
    }
}

application {
    mainClass.set("io.rippledown.server.OpenRDRServerKt")
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
    from(webpackTask.outputDirectory)

    manifest.attributes["Main-Class"] = "io.rippledown.server.OpenRDRServerKt"
}

distributions {
    main {
        contents {
            from("${layout.buildDirectory}/libs") {
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