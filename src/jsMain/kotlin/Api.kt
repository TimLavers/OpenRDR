import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.rippledown.constants.api.*
import io.rippledown.model.*
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.diff.DiffList
import kotlinx.browser.window
import kotlinx.serialization.json.Json
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

    suspend fun kbInfo() = jsonClient.get("$endpoint$KB_INFO").body<KBInfo>()

    fun importKBFromZip(file: File) {
        val code: dynamic = js("window.doZipUpload")
        val zipImportURL = "$endpoint$IMPORT_KB"
            println("++++++++++++++++ about to call zip import")
            code(zipImportURL,file)
            println("++++++++++++++++ zip import done")
    }

    fun exportURL(): String {
        return "$endpoint/api/exportKB"
    }

    suspend fun exportKBToZip() {
            println("++++++++++++++++ about to call zip export")
        val response = jsonClient.get("$endpoint$EXPORT_KB")
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

    suspend fun getCase(id: String): ViewableCase = jsonClient.get("$endpoint$CASE?id=$id").body()

    suspend fun interpretationChanges(caseName: String): DiffList =
        jsonClient.get("$endpoint$DIFF?$CASE_NAME=$caseName").body()

    suspend fun waitingCasesInfo(): CasesInfo = jsonClient.get("$endpoint$WAITING_CASES").body()

    suspend fun saveInterpretation(interpretation: Interpretation): OperationResult {
        return jsonClient.post("$endpoint$INTERPRETATION_SUBMITTED") {
            contentType(ContentType.Application.Json)
            setBody(interpretation)
        }.body()
    }

    suspend fun saveVerifiedInterpretation(verifiedInterpretation: Interpretation) {
        jsonClient.post("$endpoint$VERIFIED_INTERPRETATION_SAVED") {
            contentType(ContentType.Application.Json)
            setBody(verifiedInterpretation)
        }
    }

    suspend fun moveAttributeJustBelowOther(moved: Attribute, target: Attribute): OperationResult {
        return jsonClient.post("$endpoint$MOVE_ATTRIBUTE_JUST_BELOW_OTHER") {
            contentType(ContentType.Application.Json)
            setBody(Pair(moved, target))
        }.body()
    }
}

