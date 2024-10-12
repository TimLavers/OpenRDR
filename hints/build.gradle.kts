apply(from = "../repositories.gradle.kts")

dependencies {
    implementation(libs.bundles.kotlinx)
    implementation(libs.generativeai)
    implementation(libs.bundles.ktor)
    testImplementation(libs.kotestAssertions)

}
