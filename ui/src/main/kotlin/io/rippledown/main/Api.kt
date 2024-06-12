package io.rippledown.main

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.rippledown.constants.api.*
import io.rippledown.constants.server.KB_ID
import io.rippledown.model.CasesInfo
import io.rippledown.model.KBInfo
import io.rippledown.model.OperationResult
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
import io.rippledown.model.rule.UpdateCornerstoneRequest
import io.rippledown.sample.SampleKB
import java.io.File


class Api(engine: HttpClientEngine = CIO.create()) {
    private var currentKB: KBInfo? = null
    private val client = HttpClient(engine) {
        install(ContentNegotiation) {
            json()
        }
    }

    suspend fun HttpRequestBuilder.setKBParameter() {
        parameter(KB_ID, kbInfo().id)
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

    suspend fun createKBFromSample(name: String, sample: SampleKB): KBInfo {
        currentKB = client.post("$API_URL$CREATE_KB_FROM_SAMPLE") {
            contentType(ContentType.Application.Json)
            setBody(Pair(name, sample))
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

    suspend fun importKBFromZip(file: File): KBInfo {
        val data = file.readBytes()
        currentKB = client.post("$API_URL$IMPORT_KB") {
            contentType(ContentType.Application.Zip)
            setBody(MultiPartFormDataContent(
                formData {
                    append(
                        "document",
                        data,
                        Headers.build {
                            append(HttpHeaders.ContentType, "images/*") // Mime type required
                            append(HttpHeaders.ContentDisposition, "filename=${file.name}")
                        }
                    )
                }
            ))
        }.body()
        return currentKB!!
    }

    suspend fun exportKBToZip(destination: File) {
        val bytes = client.get("$API_URL/api/exportKB") {
            setKBParameter()
        }.body<ByteArray>()
        destination.writeBytes(bytes)
    }

    suspend fun getCase(id: Long): ViewableCase? {
        return try {
            client.get("$API_URL$CASE?id=$id") {
                setKBParameter()
            }.body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteCase(caseName: String) {
        client.delete("$API_URL$DELETE_CASE_WITH_NAME") {
            parameter("name", caseName)
            setKBParameter()
        }
    }

    suspend fun waitingCasesInfo(): CasesInfo = client.get("$API_URL$WAITING_CASES") {
        setKBParameter()
    }.body()

    suspend fun moveAttribute(moved: Int, target: Int): OperationResult {
        return client.post("$API_URL$MOVE_ATTRIBUTE") {
            contentType(ContentType.Application.Json)
            setBody(Pair(moved, target))
            setKBParameter()
        }.body()
    }

    /**
     * @return the interpretation containing the DiffList of the original and verified interpretation
     */
    suspend fun saveVerifiedInterpretation(case: ViewableCase): ViewableCase {
        val returned = client.post("$API_URL$VERIFIED_INTERPRETATION_SAVED") {
            contentType(ContentType.Application.Json)
            setBody(case)
            setKBParameter()
        }.body<ViewableCase>()
        return returned
    }


    /**
     * Build a rule for the selected Diff in the interpretation
     *
     * @param ruleRequest the information needed to build the rule
     * @return the updated case
     */
    suspend fun buildRule(ruleRequest: RuleRequest): ViewableCase {
        return client.post("$API_URL$BUILD_RULE") {
            contentType(ContentType.Application.Json)
            setBody(ruleRequest)
            setKBParameter()
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
            setKBParameter()
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
            setKBParameter()
        }.body()
    }

    /**
     * @return the conditions that are suggested for building a rule for the selected Diff in the case's interpretation
     */
    suspend fun conditionHints(caseId: Long): ConditionList {
        return client.get("$API_URL$CONDITION_HINTS?id=$caseId") {
            setKBParameter()
        }.body()
    }

    /**
     * Retrieves the specified cornerstone for current the rule session
     *
     * @param index the 0-based index of the cornerstone to retrieve. See CornerstoneStatus
     * @return the cornerstone
     */
    suspend fun selectCornerstone(index: Int): ViewableCase {
        return client.get("$API_URL$SELECT_CORNERSTONE?$INDEX_PARAMETER=$index") {
            setKBParameter()
        }.body()
    }

}

