pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

include("common")
include("server")
include("ui")
include("cucumber")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}