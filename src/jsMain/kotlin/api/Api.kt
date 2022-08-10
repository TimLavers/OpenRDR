package api

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.http.*
import io.rippledown.model.*

import kotlinx.browser.window
import kotlinx.serialization.json.Json

val endpoint = window.location.origin

val jsonClient = HttpClient {
    install(JsonFeature) { serializer = KotlinxSerializer(Json { allowStructuredMapKeys = true }) }
}

suspend fun getWaitingCasesInfo(): CasesInfo {
    return jsonClient.get("$endpoint/api/waitingCasesInfo")
}
suspend fun getCase(id: String): RDRCase {
    return jsonClient.get("$endpoint/api/case?id=$id")
}

suspend fun interpretationSubmitted(interpretation: Interpretation): OperationResult {
    return jsonClient.post("$endpoint/api/interpretationSubmitted") {
        contentType(ContentType.Application.Json)
        body = interpretation
    }
}
