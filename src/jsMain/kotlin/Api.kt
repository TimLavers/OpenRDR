import emotion.react.Global
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.core.*
import io.rippledown.model.*
import io.rippledown.model.caseview.ViewableCase
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.w3c.files.Blob
import org.w3c.xhr.FormData
import web.file.File

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

    fun importKBFromZip(file: File) {
            val code: dynamic = js("window.doZipUpload")
            val zipImportURL = "$endpoint/api/importKB"
            println("++++++++++++++++ about to call zip import")
            code(zipImportURL,file)
            println("++++++++++++++++ zip import done")
    }

    fun exportURL(): String {
        return "$endpoint/api/exportKB"
    }

    suspend fun exportKBToZip() {
            println("++++++++++++++++ about to call zip export")
        val response = jsonClient.get("$endpoint/api/exportKB")
        console.log("got response: ", response)
        console.log("got heraders: ", response.headers)
        console.log("got cont-disp: ", response.headers["Content-Disposition"])
        console.log("got cont-type: ", response.headers["Content-Type"])
//        val file = jsonClient.get("$endpoint/api/exportKB").body<File>()
//            println("++++++++++++++++ zip export done, got bytes, length: ${file.size}")
//        console.log("blob: ${file.size}")
//        println("blob: ${file.size}")
//        val code: dynamic = js("window.saveZip")
//        code(file)
        console.log("blobbbing done")
    }

    fun importInProgress(): Boolean {
        val inProgressFlagRaw = window.asDynamic().uploadZipInProgress.unsafeCast<Any>()
        console.log("inProgressFlagRaw: $inProgressFlagRaw")
        if (inProgressFlagRaw == undefined) {
            return false
        }
        return inProgressFlagRaw.unsafeCast<Boolean>()
    }

    suspend fun getCase(id: String): ViewableCase = jsonClient.get("$endpoint/api/case?id=$id").body()

    suspend fun waitingCasesInfo(): CasesInfo = jsonClient.get("$endpoint/api/waitingCasesInfo").body()

    suspend fun saveInterpretation(interpretation: Interpretation): OperationResult {
        return jsonClient.post("$endpoint/api/interpretationSubmitted") {
            contentType(ContentType.Application.Json)
            setBody(interpretation)
        }.body()
    }

    suspend fun moveAttributeJustBelowOther(moved: Int, target: Int): OperationResult {
        return jsonClient.post("$endpoint/api/moveAttributeJustBelowOther") {
            contentType(ContentType.Application.Json)
            setBody(Pair(moved, target))
        }.body()
    }
}

