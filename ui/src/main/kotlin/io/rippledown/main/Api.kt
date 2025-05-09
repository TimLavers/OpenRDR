package io.rippledown.main

import androidx.compose.runtime.InternalComposeApi
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.ContentType.Text.Plain
import io.ktor.serialization.kotlinx.json.*
import io.rippledown.constants.api.*
import io.rippledown.constants.server.CASE_ID
import io.rippledown.constants.server.EXPRESSION
import io.rippledown.constants.server.KB_ID
import io.rippledown.model.CasesInfo
import io.rippledown.model.Conclusion
import io.rippledown.model.KBInfo
import io.rippledown.model.OperationResult
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.ConditionParsingResult
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

    private suspend fun HttpRequestBuilder.setKBParameter() {
        parameter(KB_ID, kbInfo().id)
    }
    private fun HttpRequestBuilder.setCaseIdParameter(caseId: Long) {
        parameter(CASE_ID, caseId)
    }

    fun shutdown() {
//        client.close()
//        engine.close()
    }

    suspend fun createKB(name: String): KBInfo {
        currentKB = client.post("$API_URL$CREATE_KB") {
            contentType(Plain)
            setBody(name)
        }.body()
        return currentKB!!
    }

    suspend fun createKBFromSample(name: String, sample: SampleKB): KBInfo {
        currentKB = client.post("$API_URL$CREATE_KB_FROM_SAMPLE") {
            contentType(Json)
            setBody(Pair(name, sample))
        }.body()
        return currentKB!!
    }

    suspend fun selectKB(id: String): KBInfo {
        currentKB = client.post("$API_URL$SELECT_KB") {
            contentType(Plain)
            setBody(id)
        }.body()
        return currentKB!!
    }

    @OptIn(InternalComposeApi::class)
    suspend fun kbInfo(): KBInfo {
        if (currentKB == null) {
            currentKB = client.get("$API_URL$DEFAULT_KB").body<KBInfo>()
        }
        return currentKB!!
    }

    suspend fun kbList() = client.get("$API_URL$KB_LIST").body<List<KBInfo>>()

    suspend fun kbDescription() : String {
        return client.get("$API_URL$KB_DESCRIPTION") {
            contentType(Plain)
            setKBParameter()
        }.body()
    }

    suspend fun setKbDescription(description: String) {
        client.post("$API_URL$KB_DESCRIPTION") {
            contentType(Plain)
            setKBParameter()
            setBody(description)
        }
    }

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

    suspend fun getCase(caseId: Long): ViewableCase? {
        return try {
            val result: ViewableCase = client.get("$API_URL$CASE") {
                setKBParameter()
                setCaseIdParameter(caseId)
            }.body()
            result
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

    suspend fun allConclusions(): Set<Conclusion> = client.get("$API_URL$ALL_CONCLUSIONS") {
        setKBParameter()
    }.body()

    suspend fun moveAttribute(moved: Int, target: Int): OperationResult {
        return client.post("$API_URL$MOVE_ATTRIBUTE") {
            contentType(Json)
            setBody(Pair(moved, target))
            setKBParameter()
        }.body()
    }

    /**
     * Build a rule for the selected Diff in the interpretation
     *
     * @param ruleRequest the information needed to build the rule
     * @return the updated case
     */
    suspend fun commitSession(ruleRequest: RuleRequest): ViewableCase {
        return client.post("$API_URL$COMMIT_RULE_SESSION") {
            contentType(Json)
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
            contentType(Json)
            setBody(sessionStartRequest)
            setKBParameter()
        }.body<CornerstoneStatus>()
        return body
    }

    /**
     * Cancels the current rule session
     */
    suspend fun cancelRuleSession(): HttpStatusCode {
        client.post("$API_URL$CANCEL_RULE_SESSION") {
            setKBParameter()
        }
        return HttpStatusCode.OK
    }

    /**
     * Updates the CornerstoneStatus for the current rule session when the conditions are changed
     *
     * @return the updated CornerstoneStatus
     */
    suspend fun updateCornerstoneStatus(updateCornerstoneRequest: UpdateCornerstoneRequest): CornerstoneStatus {
        return client.post("$API_URL$UPDATE_CORNERSTONES") {
            contentType(Json)
            setBody(updateCornerstoneRequest)
            setKBParameter()
        }.body()
    }

    /**
     * Retrieves the updated cornerstone status for the current rule session
     *
     * @param index the 0-based index of the cornerstone that has been selected.
     */
    suspend fun selectCornerstone(index: Int): CornerstoneStatus {
        return client.get("$API_URL$SELECT_CORNERSTONE") {
            contentType(Json)
            setBody(index)
            setKBParameter()
        }.body()
    }

    /**
     * Exempts the cornerstone at the specified index for the current rule session
     *
     * @return the updated CornerstoneStatus
     */
    suspend fun exemptCornerstone(index: Int): CornerstoneStatus {
        return client.post("$API_URL$EXEMPT_CORNERSTONE") {
            contentType(Json)
            setBody(index)
            setKBParameter()
        }.body()
    }

    /**
     * @return the conditions that are suggested for building a rule
     */
    suspend fun conditionHints(caseId: Long): ConditionList {
        return client.get("$API_URL$CONDITION_HINTS") {
            setKBParameter()
            setCaseIdParameter(caseId)
        }.body()
    }

    /**
     * @return a condition that corresponds to the specified expression
     */
    suspend fun conditionFor(expression: String, attributeNames: Collection<String>): ConditionParsingResult {
        return client.get("$API_URL$CONDITION_FOR_EXPRESSION") {
            contentType(Json)
            setKBParameter()
            parameter(EXPRESSION, expression)
            setBody(attributeNames)
        }.body<ConditionParsingResult>()
    }

    suspend fun sendUserMessage(message: String, caseId: Long): String {
        return client.post("$API_URL$SEND_USER_MESSAGE") {
            contentType(Plain)
            parameter(CASE_ID, caseId)
            setKBParameter()
            setCaseIdParameter(caseId)
            setBody(message)
        }.body()
    }


}

