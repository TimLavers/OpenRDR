import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.rippledown.model.*
import io.rippledown.model.caseview.ViewableCase
import kotlinx.browser.window
import kotlinx.serialization.json.Json

val endpoint = window.location.origin

class Api(engine: HttpClientEngine = Js.create()) {
    val jsonClient = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                allowStructuredMapKeys = true
            })
        }
    }

    suspend fun kbInfo() = jsonClient.get("$endpoint/api/kbInfo").body<KBInfo>()

    suspend fun getCase(id: String): ViewableCase = jsonClient.get("$endpoint/api/case?id=$id").body()

    suspend fun waitingCasesInfo(): CasesInfo = jsonClient.get("$endpoint/api/waitingCasesInfo").body()

    suspend fun saveInterpretation(interpretation: Interpretation): OperationResult {
        return jsonClient.post("$endpoint/api/interpretationSubmitted") {
            contentType(ContentType.Application.Json)
            setBody(interpretation)
        }.body()
    }

    suspend fun moveAttributeJustBelowOther(moved:Attribute, target: Attribute): OperationResult {
        return jsonClient.post("$endpoint/api/moveAttributeJustBelowOther") {
            contentType(ContentType.Application.Json)
            setBody(Pair(moved, target))
        }.body()
    }
}

