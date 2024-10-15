apply(from = "../repositories.gradle.kts")

dependencies {
    implementation(libs.generativeai)
    implementation(libs.ktorClientMock)
    testImplementation(libs.kotestAssertions)

}
