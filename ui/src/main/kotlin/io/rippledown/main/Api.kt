package io.rippledown.main

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.rippledown.constants.api.*
import io.rippledown.model.CasesInfo
import io.rippledown.model.KBInfo
import io.rippledown.model.OperationResult
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
import io.rippledown.model.rule.UpdateCornerstoneRequest
import java.io.File

class Api(engine: HttpClientEngine = CIO.create()) {
    private var currentKB: KBInfo? = null
    private val client = HttpClient(engine) {
        install(ContentNegotiation) {
            json()
        }
    }

    fun shutdown() {
        client.close()
    }

    suspend fun createKB(name: String): KBInfo {
        currentKB = client.post("$API_URL$CREATE_KB") {
            contentType(ContentType.Text.Plain)
            setBody(name)
        }.body()
        return currentKB!!
    }

    suspend fun selectKB(id: String): KBInfo {
        currentKB = client.post("$API_URL$SELECT_KB") {
            contentType(ContentType.Text.Plain)
            setBody(id)
        }.body()
        return currentKB!!
    }

    suspend fun kbInfo(): KBInfo {
        if (currentKB == null) {
            currentKB = client.get("$API_URL$DEFAULT_KB").body<KBInfo>()
        }
        return currentKB!!
    }

    suspend fun kbList() = client.get("$API_URL$KB_LIST").body<List<KBInfo>>()

    fun importKBFromZip(file: File) {
//        val code: dynamic = js("window.doZipUpload")
//        val zipImportURL = "$API_URL$IMPORT_KB"
//        code(zipImportURL, file)
    }

    fun exportURL(): String {
        return "$API_URL/api/exportKB"
    }

    suspend fun exportKBToZip() {
        println("++++++++++++++++ about to call zip export")
        val response = client.get("$API_URL$EXPORT_KB")
//        debug("got response: ", response)
//        debug("got heraders: ", response.headers)
//        debug("got cont-disp: ", response.headers["Content-Disposition"])
//        debug("got cont-type: ", response.headers["Content-Type"])
//        val file = jsonClient.get("$API_URL/api/exportKB").body<File>()
//            println("++++++++++++++++ zip export done, got bytes, length: ${file.size}")
//        debug("blob: ${file.size}")
//        println("blob: ${file.size}")
//        val code: dynamic = js("window.saveZip")
//        code(file)
        debug("blobbbing done")
    }

    fun importInProgress(): Boolean {
//        val inProgressFlagRaw = window.asDynamic().uploadZipInProgress.unsafeCast<Any>()
//        debug("inProgressFlagRaw: $inProgressFlagRaw")
//        if (inProgressFlagRaw == undefined) {
//            return false
//        }
//        return inProgressFlagRaw.unsafeCast<Boolean>()
        return true
    }

    suspend fun getCase(id: Long): ViewableCase? {
        try {
           return client.get("$API_URL$CASE?id=$id") {
                parameter("KB", kbId())
            }.body()
        } catch (e: Exception) {
            return null
        }
    }

    suspend fun deleteCase(caseName: String) {
        client.delete("$API_URL$DELETE_CASE_WITH_NAME") {
            parameter("KB", kbId())
            parameter("name", caseName)
        }
    }

    suspend fun waitingCasesInfo(): CasesInfo = client.get("$API_URL$WAITING_CASES") {
        parameter("KB", kbId())
    }.body()

    suspend fun moveAttributeJustBelowOther(moved: Int, target: Int): OperationResult {
        return client.post("$API_URL$MOVE_ATTRIBUTE_JUST_BELOW_OTHER") {
            contentType(ContentType.Application.Json)
            setBody(Pair(moved, target))
        }.body()
    }

    /**
     * @return the interpretation containing the DiffList of the original and verified interpretation
     */
    suspend fun saveVerifiedInterpretation(case: ViewableCase): ViewableCase {
        val returned = client.post("$API_URL$VERIFIED_INTERPRETATION_SAVED") {
            contentType(ContentType.Application.Json)
            setBody(case)
            parameter("KB", kbId())
        }.body<ViewableCase>()
        return returned
    }

    /**
     * Build a rule for the selected Diff in the interpretation
     *
     * @param ruleRequest the information needed to build the rule
     * @return the updated interpretation
     */
    suspend fun buildRule(ruleRequest: RuleRequest): ViewableInterpretation {
        return client.post("$API_URL$BUILD_RULE") {
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
        val body = client.post("$API_URL$START_RULE_SESSION") {
            contentType(ContentType.Application.Json)
            setBody(sessionStartRequest)
        }.body<CornerstoneStatus>()
        return body
    }

    /**
     * Updates the CornerstoneStatus for the current rule session when the conditions are changed
     *
     * @return the updated CornerstoneStatus
     */
    suspend fun updateCornerstoneStatus(updateCornerstoneRequest: UpdateCornerstoneRequest): CornerstoneStatus {
        return client.post("$API_URL$UPDATE_CORNERSTONES") {
            contentType(ContentType.Application.Json)
            setBody(updateCornerstoneRequest)
        }.body()
    }

    /**
     * @return the conditions that are suggested for building a rule for the selected Diff in the case's interpretation
     */
    suspend fun conditionHints(caseId: Long): ConditionList =
        client.get("$API_URL$CONDITION_HINTS?id=$caseId").body()

    /**
     * Retrieves the specified cornerstone for current the rule session
     *
     * @param index the 0-based index of the cornerstone to retrieve
     * @return the cornerstone, its index and the total number of cornerstones
     */
    suspend fun selectCornerstone(index: Int): CornerstoneStatus {
        return client.get("$API_URL$SELECT_CORNERSTONE?$INDEX_PARAMETER=$index").body()
    }

    private suspend fun kbId() = kbInfo().id
}

