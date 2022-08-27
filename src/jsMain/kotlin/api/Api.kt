package api

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.rippledown.model.CasesInfo
import io.rippledown.model.Interpretation
import io.rippledown.model.OperationResult
import io.rippledown.model.RDRCase
import kotlinx.browser.window
import kotlinx.serialization.json.Json

val endpoint = window.location.origin

val jsonClient = HttpClient {
    install(JsonFeature) { serializer = KotlinxSerializer(Json { allowStructuredMapKeys = true }) }
}

suspend fun getWaitingCasesInfo(): CasesInfo = jsonClient.get("$endpoint/api/waitingCasesInfo")

suspend fun getCase(id: String): RDRCase = jsonClient.get("$endpoint/api/case?id=$id")

suspend fun saveInterpretation(interpretation: Interpretation): OperationResult {
    return jsonClient.post("$endpoint/api/interpretationSubmitted") {
        contentType(ContentType.Application.Json)
        body = interpretation
    }
}
