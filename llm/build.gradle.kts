
dependencies {
    implementation(project(path = ":common"))
    implementation(libs.bundles.kotlinx)
    implementation(libs.logback)
    implementation(libs.generativeai)
    testImplementation(libs.kotestAssertions)
}