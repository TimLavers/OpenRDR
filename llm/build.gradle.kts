plugins {
    id("kotlin-library-conventions")
    kotlin("plugin.serialization")
}

dependencies {
    // Export the SDK's security constraints to every module that uses this library.
    api(platform(libs.jacksonBom))
    constraints {
        api(libs.commonsCodec) {
            because("The GenAI SDK's HTTP client requests vulnerable Commons Codec 1.11")
        }
    }
    implementation(project(path = ":common"))
    implementation(libs.bundles.kotlinx)
    implementation(libs.logback)
    implementation(libs.generativeai)
    testImplementation(libs.kotestAssertions)
    testImplementation(libs.kotlinxCoroutinesTest)
}
