sourceSets {
    main {
        resources {
            srcDir("src/main/resources")
            srcDir(rootProject.projectDir.resolve("shared-resources"))
        }
    }
}