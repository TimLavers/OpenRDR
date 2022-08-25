package api

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.rippledown.model.CasesInfo
import io.rippledown.model.Interpretation
import io.rippledown.model.RDRCase
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

interface Api {
    fun interpretationSubmitted(interpretation: Interpretation)
}

class ApiImpl : Api {
    private val scope = MainScope()

    val endpoint = window.location.origin

    val jsonClient = HttpClient {
        install(JsonFeature) { serializer = KotlinxSerializer() }
    }

    suspend fun getWaitingCasesInfo(): CasesInfo {
        return jsonClient.get("$endpoint/api/waitingCasesInfo")
    }

    suspend fun getCase(id: String): RDRCase = jsonClient.get("$endpoint/api/case?id=$id")

    override fun interpretationSubmitted(interpretation: Interpretation) {
        scope.launch {
            jsonClient.post("$endpoint/api/interpretationSubmitted") {
                contentType(ContentType.Application.Json)
                body = interpretation
            }
        }
    }
}
