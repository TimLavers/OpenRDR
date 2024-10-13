apply(from = "../repositories.gradle.kts")

dependencies {
    implementation(libs.bundles.kotlinx)
    implementation("io.ktor:ktor-client-core:3.0.0") //needed by generativeai
    implementation(libs.generativeai)
    implementation(libs.ktorClientMock)
    testImplementation(libs.kotestAssertions)

}
