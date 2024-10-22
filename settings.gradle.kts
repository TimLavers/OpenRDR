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
include("hints")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}