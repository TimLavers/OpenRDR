import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.rippledown.constants.api.*
import io.rippledown.model.CasesInfo
import io.rippledown.model.Interpretation
import io.rippledown.model.KBInfo
import io.rippledown.model.OperationResult
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
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
        code(zipImportURL, file)
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

    suspend fun getCase(id: Long): ViewableCase = jsonClient.get("$endpoint$CASE?id=$id").body()

    suspend fun waitingCasesInfo(): CasesInfo = jsonClient.get("$endpoint$WAITING_CASES").body()

    /**
     * @return the interpretation containing the DiffList of the original and verified interpretation
     */
    suspend fun saveVerifiedInterpretation(verifiedInterpretation: Interpretation): Interpretation {
        return jsonClient.post("$endpoint$VERIFIED_INTERPRETATION_SAVED") {
            contentType(ContentType.Application.Json)
            setBody(verifiedInterpretation)
        }.body()
    }

    /**
     * Build a rule for the selected Diff in the interpretation
     *
     * @param ruleRequest the information needed to build the rule
     * @return the updated interpretation
     * @deprecated use [startRuleSession] instead
     */
    suspend fun buildRule(ruleRequest: RuleRequest): Interpretation {
        return jsonClient.post("$endpoint$BUILD_RULE") {
            contentType(ContentType.Application.Json)
            setBody(ruleRequest)
        }.body()
    }

    /**
     * Starts a rule session for the specified Diff
     *
     * @param sessionStartRequest the information needed to start the rule session
     * @return the first cornerstone, its index and the total number of cornerstones
     */
    suspend fun startRuleSession(sessionStartRequest: SessionStartRequest): CornerstoneStatus {
        return jsonClient.post("$endpoint$START_RULE_SESSION") {
            contentType(ContentType.Application.Json)
            setBody(sessionStartRequest)
        }.body()
    }

    /**
     * @return the conditions that are suggested for building a rule for the selected Diff in the case's interpretation
     */
    suspend fun conditionHints(caseId: Long): ConditionList =
        jsonClient.get("$endpoint$CONDITION_HINTS?id=$caseId").body()

    suspend fun moveAttributeJustBelowOther(moved: Int, target: Int): OperationResult {
        return jsonClient.post("$endpoint$MOVE_ATTRIBUTE_JUST_BELOW_OTHER") {
            contentType(ContentType.Application.Json)
            setBody(Pair(moved, target))
        }.body()
    }


}

