package io.rippledown.mocks

import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.*
import io.rippledown.constants.api.*
import io.rippledown.constants.server.CASE_ID
import io.rippledown.constants.server.EXPRESSION
import io.rippledown.model.CasesInfo
import io.rippledown.model.Conclusion
import io.rippledown.model.KBInfo
import io.rippledown.model.OperationResult
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.model.rule.*
import io.rippledown.sample.SampleKB
import io.rippledown.utils.createViewableCase
import kotlinx.serialization.json.Json

fun mock(config: EngineConfig) = EngineBuilder(config).build()

fun config(block: EngineConfig.() -> Unit) = EngineConfig().apply(block)

class EngineConfig {
    var returnedKbDescription = "A fine KB!"
    var returnCasesInfo: CasesInfo = CasesInfo(emptyList())
    var returnCase: ViewableCase? = createViewableCase("The Case")
    var returnOperationResult: OperationResult = OperationResult()
    var returnCaseAfterBuildingRule: ViewableCase = createViewableCase("The Case")
    var returnCornerstone: ViewableCase = createViewableCase("The Case")
    var returnCornerstoneStatus: CornerstoneStatus = CornerstoneStatus()
    var returnConditionList: ConditionList = ConditionList()
    var returnResponse: String = ""

    var returnConditionParsingResult: ConditionParsingResult? = null
    var expectedCaseId: Long? = null
    var expectedCase: ViewableCase? = null
    var expectedRuleRequest: RuleRequest? = null
    var expectedSessionStartRequest: SessionStartRequest? = null
    var expectedSessionCancel: Boolean? = false
    var expectedCornerstoneSelection: Int? = -1
    var expectedUpdateCornerstoneRequest: UpdateCornerstoneRequest? = null
    var expectedCornerstoneIndex: Int? = null
    var expectedUpdatedCornerstoneStatus: CornerstoneStatus? = null
    var expectedExpression: String = ""

    var expectedAttributeNames: Collection<String> = emptyList()
    var expectedMovedAttributeId: Int? = null
    var expectedTargetAttributeId: Int? = null
    var expectedUserMessage: String = ""
    var newKbName: String? = null
    var sampleKB: SampleKB? = null

    var undoRuleDescription: UndoRuleDescription = UndoRuleDescription("It was a great rule, but it has to go.", true)
    var lastRuleUndoCalled = false

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
                if (config.expectedCaseId != null) request.url.parameters[CASE_ID] shouldBe config.expectedCaseId.toString()
                httpResponseData(json.encodeToString(config.returnCase))
            }

            COMMIT_RULE_SESSION -> {
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

            CANCEL_RULE_SESSION -> {
                if (config.expectedSessionCancel != null) {
                    config.expectedSessionCancel shouldBe true
                }
                httpResponseData(HttpStatusCode.OK.toString())
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

            EXEMPT_CORNERSTONE, SELECT_CORNERSTONE -> {
                val body = request.body as TextContent
                val bodyAsCornerstoneIndex = json.decodeFromString<Int>(body.text)

                if (config.expectedCornerstoneIndex != null) {
                    bodyAsCornerstoneIndex shouldBe config.expectedCornerstoneIndex
                }
                httpResponseData(json.encodeToString(config.returnCornerstoneStatus))
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
                if (config.expectedCaseId != null) request.url.parameters[CASE_ID] shouldBe config.expectedCaseId.toString()
                httpResponseData(json.encodeToString(config.returnConditionList))
            }

            CONDITION_FOR_EXPRESSION -> {
                request.url.parameters[EXPRESSION] shouldBe config.expectedExpression
                httpResponseData(json.encodeToString(config.returnConditionParsingResult))
            }

            KB_DESCRIPTION -> {
                if (request.method == HttpMethod.Get) {
                    httpResponseData(config.returnedKbDescription)
                } else {
                    config.returnedKbDescription = (request.body as TextContent).text
                    httpResponseData("OK")
                }
            }

            LAST_RULE_DESCRIPTION -> {
                if (request.method == HttpMethod.Get) {
                    httpResponseData(json.encodeToString(config.undoRuleDescription))
                } else if (request.method == HttpMethod.Delete){
                    config.lastRuleUndoCalled = true
                    httpResponseData("OK")
                } else {
                    httpResponseData("No way!")
                }
            }

            DEFAULT_KB -> {
                httpResponseData(json.encodeToString(config.defaultKB))
            }

            SEND_USER_MESSAGE -> {
                val body = request.body as TextContent
                body.text shouldBe config.expectedUserMessage
                request.url.parameters[CASE_ID] shouldBe config.expectedCaseId.toString()
                httpResponseData(config.returnResponse)
            }

            START_CONVERSATION -> {
                request.url.parameters[CASE_ID] shouldBe config.expectedCaseId.toString()
                httpResponseData(config.returnResponse)
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
