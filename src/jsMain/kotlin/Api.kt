import io.ktor.http.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.rippledown.model.CasesInfo
import io.rippledown.model.Interpretation
import io.rippledown.model.OperationResult
import io.rippledown.model.RDRCase
import kotlinx.browser.window

val endpoint = window.location.origin

class Api(engine: HttpClientEngine = Js.create()) {
    val jsonClient = HttpClient(engine) {
        install(ContentNegotiation) {
            json()
        }
    }

    suspend fun getCase(id: String): RDRCase = jsonClient.get("$endpoint/api/case?id=$id").body()

    suspend fun waitingCasesInfo(): CasesInfo = jsonClient.get("$endpoint/api/waitingCasesInfo").body()

    suspend fun saveInterpretation(interpretation: Interpretation): OperationResult {
        return jsonClient.post("$endpoint/api/interpretationSubmitted") {
            contentType(ContentType.Application.Json)
            setBody(interpretation)
        }.body()
    }

}

