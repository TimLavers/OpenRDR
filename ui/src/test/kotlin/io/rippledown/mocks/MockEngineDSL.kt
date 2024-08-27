package io.rippledown.mocks

import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.*
import io.rippledown.constants.api.*
import io.rippledown.model.*
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
import io.rippledown.model.rule.UpdateCornerstoneRequest
import io.rippledown.sample.SampleKB
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun mock(config: EngineConfig) = EngineBuilder(config).build()

fun config(block: EngineConfig.() -> Unit) = EngineConfig().apply(block)

class EngineConfig {
    var returnCasesInfo: CasesInfo = CasesInfo(emptyList())
    var returnCase: ViewableCase? = createCase("The Case")
    var returnOperationResult: OperationResult = OperationResult()
    var returnCaseAfterBuildingRule: ViewableCase = createCase("The Case")
    var returnCornerstone: ViewableCase = createCase("The Case")
    var returnCornerstoneStatus: CornerstoneStatus = CornerstoneStatus()
    var returnConditionList: ConditionList = ConditionList()

    var expectedCaseId: Long? = null
    var expectedCase: ViewableCase? = null
    var expectedRuleRequest: RuleRequest? = null
    var expectedSessionStartRequest: SessionStartRequest? = null
    var expectedCornerstoneSelection: Int? = -1
    var expectedUpdateCornerstoneRequest: UpdateCornerstoneRequest? = null
    var expectedCornerstoneIndex: Int? = null
    var expectedUpdatedCornerstoneStatus: CornerstoneStatus? = null

    var expectedMovedAttributeId: Int? = null
    var expectedTargetAttributeId: Int? = null
    var newKbName: String? = null
    var sampleKB: SampleKB? = null

    val defaultKB = KBInfo("Thyroids")
    var returnKBInfo = defaultKB
    val returnKBList = listOf(KBInfo("Glucose"), KBInfo("Lipids"), defaultKB)
    lateinit var returnConclusions: Set<Conclusion>
}

private class EngineBuilder(private val config: EngineConfig) {
    private val json = Json {
        allowStructuredMapKeys = true
    }

    fun build() = MockEngine { request ->
        when (request.url.encodedPath) {
            ALL_CONCLUSIONS -> {
                httpResponseData(json.encodeToString(config.returnConclusions))
            }

            WAITING_CASES -> {
                httpResponseData(json.encodeToString(config.returnCasesInfo))
            }

            CASE -> {
                if (config.expectedCaseId != null) request.url.parameters["id"] shouldBe config.expectedCaseId.toString()
                httpResponseData(json.encodeToString(config.returnCase))
            }

            BUILD_RULE -> {
                val body = request.body as TextContent
                val bodyAsRuleRequest = json.decodeFromString(RuleRequest.serializer(), body.text)

                if (config.expectedRuleRequest != null) {
                    bodyAsRuleRequest shouldBe config.expectedRuleRequest
                }
                httpResponseData(json.encodeToString(config.returnCaseAfterBuildingRule))
            }

            START_RULE_SESSION -> {
                val body = request.body as TextContent
                val bodyAsSessionStartRequest = json.decodeFromString(SessionStartRequest.serializer(), body.text)

                if (config.expectedSessionStartRequest != null) {
                    bodyAsSessionStartRequest shouldBe config.expectedSessionStartRequest
                }
                httpResponseData(json.encodeToString(config.returnCornerstoneStatus))
            }

            UPDATE_CORNERSTONES -> {
                val body = request.body as TextContent
                val bodyAsUpdateCornerstoneRequest =
                    json.decodeFromString(UpdateCornerstoneRequest.serializer(), body.text)

                if (config.expectedUpdateCornerstoneRequest != null) {
                    bodyAsUpdateCornerstoneRequest shouldBe config.expectedUpdateCornerstoneRequest
                }
                httpResponseData(json.encodeToString(config.returnCornerstoneStatus))
            }

            EXEMPT_CORNERSTONE -> {
                val body = request.body as TextContent
                val bodyAsCornerstoneIndex = json.decodeFromString<Int>(body.text)

                if (config.expectedCornerstoneIndex != null) {
                    bodyAsCornerstoneIndex shouldBe config.expectedCornerstoneIndex
                }
                httpResponseData(json.encodeToString(config.returnCornerstoneStatus))
            }

            SELECT_CORNERSTONE -> {
                if (config.expectedCornerstoneSelection != -1) request.url.parameters[INDEX_PARAMETER] shouldBe config.expectedCornerstoneSelection.toString()
                httpResponseData(json.encodeToString(config.returnCornerstone))
            }

            MOVE_ATTRIBUTE -> {
                val body = request.body as TextContent
                val data = Json.decodeFromString<Pair<Int, Int>>(body.text)
                data.first shouldBe config.expectedMovedAttributeId
                data.second shouldBe config.expectedTargetAttributeId
                httpResponseData(json.encodeToString(config.returnOperationResult))
            }

            KB_INFO -> {
                println("api call to KB_INFO")
                httpResponseData(json.encodeToString(config.returnKBInfo))
            }

            KB_LIST -> {
                httpResponseData(json.encodeToString(config.returnKBList))
            }

            CREATE_KB -> {
                val body = request.body as TextContent
                val name = body.text
                config.newKbName = name
                httpResponseData(json.encodeToString(KBInfo(name)))
            }

            CREATE_KB_FROM_SAMPLE -> {
                val body = request.body as TextContent
                val data = Json.decodeFromString<Pair<String, SampleKB>>(body.text)
                config.newKbName = data.first
                config.sampleKB = data.second

                httpResponseData(json.encodeToString(KBInfo(data.first)))
            }

            CONDITION_HINTS -> {
                if (config.expectedCaseId != null) request.url.parameters["id"] shouldBe config.expectedCaseId.toString()
                httpResponseData(json.encodeToString(config.returnConditionList))
            }

            DEFAULT_KB -> {
                httpResponseData(json.encodeToString(config.defaultKB))
            }

            else -> {
                error("Unhandled ${request.url.fullPath}")
            }
        }
    }

    private fun MockRequestHandleScope.httpResponseData(dataToSend: String) = respond(
        content = ByteReadChannel(dataToSend),
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json")
    )
}
