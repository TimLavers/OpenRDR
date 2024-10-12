pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

include("common")
include("cucumber")
include("server")
include("hints")
include("ui")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}