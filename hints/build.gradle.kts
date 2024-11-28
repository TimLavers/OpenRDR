apply(from = "../repositories.gradle.kts")

dependencies {
    implementation(project(":common"))
    implementation(kotlin("reflect"))
    implementation(libs.logback)
    implementation(libs.generativeai)
    implementation(libs.ktorClientMock)
    testImplementation(libs.kotestAssertions)
    testImplementation(libs.mockk)

}
