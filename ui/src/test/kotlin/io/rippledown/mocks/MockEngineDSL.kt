package io.rippledown.mocks

import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.*
import io.rippledown.constants.api.*
import io.rippledown.main.Api
import io.rippledown.model.CasesInfo
import io.rippledown.model.KBInfo
import io.rippledown.model.OperationResult
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.createCase
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
import io.rippledown.model.rule.UpdateCornerstoneRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun mock(config: EngineConfig)  = EngineBuilder(config).build()

val engineConfig = EngineConfig()
val defaultMock = mock(engineConfig)

fun config(block: EngineConfig.() -> Unit) = EngineConfig().apply(block)

class EngineConfig {
    var returnCasesInfo: CasesInfo = CasesInfo(emptyList())
    var returnCase: ViewableCase = createCase("The Case")
    var returnOperationResult: OperationResult = OperationResult()
    var returnInterpretationAfterSavingInterpretation: ViewableInterpretation = ViewableInterpretation()
    var returnInterpretationAfterBuildingRule: ViewableInterpretation = ViewableInterpretation()
    var returnCornerstoneStatus: CornerstoneStatus = CornerstoneStatus()
    var returnConditionList: ConditionList = ConditionList()

    var expectedCaseId: Long? = null
    var expectedInterpretation: ViewableInterpretation? = null
    var expectedRuleRequest: RuleRequest? = null
    var expectedSessionStartRequest: SessionStartRequest? = null
    var expectedCornerstoneSelection: Int? = -1
    var expectedUpdateCornerstoneRequest: UpdateCornerstoneRequest? = null

    var expectedMovedAttributeId: Int? = null
    var expectedTargetAttributeId: Int? = null
    var expectedNewProjectName: String? = null

    val defaultKB = KBInfo("Thyroids")
    var returnKBInfo = defaultKB
    val returnKBList = listOf(KBInfo("Glucose"), KBInfo("Lipids"), defaultKB)
}

private class EngineBuilder(private val config: EngineConfig) {
    private val json = Json {
        allowStructuredMapKeys = true
    }

    fun build()  = MockEngine { request ->
        println("MockEngine called with ${request.url.fullPath}")
        when (request.url.encodedPath) {
            WAITING_CASES -> {
                println("api call to WAITING_CASES with ${request.url.parameters} will return ${config.returnCasesInfo}")
                httpResponseData(json.encodeToString(config.returnCasesInfo))
            }

            CASE -> {
                if (config.expectedCaseId != null) request.url.parameters["id"] shouldBe config.expectedCaseId.toString()
                println("api call to CASE with ${request.url.parameters} will return ${config.returnCase}")
                httpResponseData(json.encodeToString(config.returnCase))
            }

            VERIFIED_INTERPRETATION_SAVED -> {
                val body = request.body as TextContent
                val bodyAsInterpretation = Json.decodeFromString(ViewableInterpretation.serializer(), body.text)

                if (config.expectedInterpretation != null) {
                    bodyAsInterpretation shouldBe config.expectedInterpretation
                }
                httpResponseData(json.encodeToString(config.returnInterpretationAfterSavingInterpretation))
            }

            BUILD_RULE -> {
                val body = request.body as TextContent
                val bodyAsRuleRequest = Json.decodeFromString(RuleRequest.serializer(), body.text)

                if (config.expectedRuleRequest != null) {
                    bodyAsRuleRequest shouldBe config.expectedRuleRequest
                }
                httpResponseData(json.encodeToString(config.returnInterpretationAfterBuildingRule))
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

            SELECT_CORNERSTONE -> {
                if (config.expectedCornerstoneSelection != -1) request.url.parameters[INDEX_PARAMETER] shouldBe config.expectedCornerstoneSelection.toString()
                httpResponseData(json.encodeToString(config.returnCornerstoneStatus))
            }

            MOVE_ATTRIBUTE_JUST_BELOW_OTHER -> {
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
                name shouldBe config.expectedNewProjectName
                httpResponseData("")
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
