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

open class Api {

    val endpoint = window.location.origin

    val jsonClient = HttpClient {
        install(JsonFeature) { serializer = KotlinxSerializer() }
    }

    suspend fun getWaitingCasesInfo(): CasesInfo {
        return jsonClient.get("$endpoint/api/waitingCasesInfo")
    }

    suspend fun getCase(id: String): RDRCase = jsonClient.get("$endpoint/api/case?id=$id")

    open suspend fun interpretationSubmitted(interpretation: Interpretation): OperationResult {
        return jsonClient.post("$endpoint/api/interpretationSubmitted") {
            contentType(ContentType.Application.Json)
            body = interpretation
        }
    }
}
