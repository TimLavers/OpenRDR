pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
rootProject.name = "OpenRDR"
include("common")
include("server")
include("ui")
include("cucumber")