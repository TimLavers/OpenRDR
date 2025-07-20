pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

include("chat")
include("common")
include("cucumber")
include("hints")
include("llm")
include("server")
include("shared-resources")
include("ui")