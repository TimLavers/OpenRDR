apply(from = "../repositories.gradle.kts")

dependencies {
    implementation(libs.bundles.kotlinx)
    implementation(libs.generativeai)
    implementation(libs.ktorClientMock)
    testImplementation(libs.kotestAssertions)

}
