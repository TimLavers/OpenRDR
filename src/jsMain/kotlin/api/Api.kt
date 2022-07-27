package api

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.http.*
import io.rippledown.model.CasesInfo
import io.rippledown.model.Interpretation
import io.rippledown.model.RDRCase

import kotlinx.browser.window

val endpoint = window.location.origin

val jsonClient = HttpClient {
    install(JsonFeature) { serializer = KotlinxSerializer() }
}

suspend fun getWaitingCasesInfo(): CasesInfo {
    return jsonClient.get("$endpoint/api/waitingCasesInfo")
}
suspend fun getCase(id: String): RDRCase {
    return jsonClient.get("$endpoint/api/case?id=$id")
}

suspend fun interpretationSubmitted(interpretation: Interpretation) {
    console.log("api...interpSub: $interpretation")
    jsonClient.post<Interpretation>("$endpoint/api/interpretationSubmitted") {
        contentType(ContentType.Application.Json)
        body = interpretation
    }
}
