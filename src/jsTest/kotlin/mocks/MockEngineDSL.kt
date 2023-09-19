package mocks

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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun mock(config: EngineConfig) = EngineBuilder(config).build()

val defaultMock = mock(EngineConfig())

fun config(block: EngineConfig.() -> Unit) = EngineConfig().apply(block)

class EngineConfig {
    var returnCasesInfo: CasesInfo = CasesInfo(emptyList())
    var returnCase: ViewableCase = createCase("The Case")
    var returnOperationResult: OperationResult = OperationResult()
    var returnInterpretationAfterSavingInterpretation: Interpretation = Interpretation()
    var returnInterpretationAfterBuildingRule: Interpretation = Interpretation()
    var returnCornerstoneStatus: CornerstoneStatus = CornerstoneStatus()
    var returnConditionList: ConditionList = ConditionList()

    var expectedCaseId: Long? = null
    var expectedInterpretation: Interpretation? = null
    var expectedRuleRequest: RuleRequest? = null
    var expectedSessionStartRequest: SessionStartRequest? = null
    var expectedCornerstoneSelection: Int? = -1
    var expectedUpdateCornerstoneRequest: UpdateCornerstoneRequest? = null

    var expectedMovedAttributeId: Int? = null
    var expectedTargetAttributeId: Int? = null

    val returnKBInfo = KBInfo("Glucose")

}

private class EngineBuilder(private val config: EngineConfig) {
    private val json = Json {
        allowStructuredMapKeys = true
    }

    fun build() = MockEngine { request ->
        when (request.url.encodedPath) {
            WAITING_CASES -> {
                respond(
                    content = ByteReadChannel(
                        json.encodeToString(config.returnCasesInfo)
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }

            CASE -> {
                if (config.expectedCaseId != null) request.url.parameters["id"] shouldBe config.expectedCaseId.toString()
                respond(
                    content = ByteReadChannel(
                        json.encodeToString(config.returnCase)
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }

            VERIFIED_INTERPRETATION_SAVED -> {
                val body = request.body as TextContent
                val bodyAsInterpretation = Json.decodeFromString(Interpretation.serializer(), body.text)

                if (config.expectedInterpretation != null) {
                    bodyAsInterpretation shouldBe config.expectedInterpretation
                }
                respond(
                    content = ByteReadChannel(
                        json.encodeToString(config.returnInterpretationAfterSavingInterpretation)
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }

            BUILD_RULE -> {
                val body = request.body as TextContent
                val bodyAsRuleRequest = Json.decodeFromString(RuleRequest.serializer(), body.text)

                if (config.expectedRuleRequest != null) {
                    bodyAsRuleRequest shouldBe config.expectedRuleRequest
                }
                respond(
                    content = ByteReadChannel(
                        json.encodeToString(config.returnInterpretationAfterBuildingRule)
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }

            START_RULE_SESSION -> {
                val body = request.body as TextContent
                val bodyAsSessionStartRequest = json.decodeFromString(SessionStartRequest.serializer(), body.text)

                if (config.expectedSessionStartRequest != null) {
                    bodyAsSessionStartRequest shouldBe config.expectedSessionStartRequest
                }
                respond(
                    content = ByteReadChannel(
                        json.encodeToString(config.returnCornerstoneStatus)
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }

            UPDATE_CORNERSTONES -> {
                val body = request.body as TextContent
                val bodyAsUpdateCornerstoneRequest =
                    json.decodeFromString(UpdateCornerstoneRequest.serializer(), body.text)

                if (config.expectedUpdateCornerstoneRequest != null) {
                    bodyAsUpdateCornerstoneRequest shouldBe config.expectedUpdateCornerstoneRequest
                }

                respond(
                    content = ByteReadChannel(
                        json.encodeToString(config.returnCornerstoneStatus)
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }

            SELECT_CORNERSTONE -> {
                if (config.expectedCornerstoneSelection != -1) request.url.parameters[INDEX_PARAMETER] shouldBe config.expectedCornerstoneSelection.toString()
                respond(
                    content = ByteReadChannel(
                        json.encodeToString(config.returnCornerstoneStatus)
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }


            MOVE_ATTRIBUTE_JUST_BELOW_OTHER -> {
                val body = request.body as TextContent
                val data = Json.decodeFromString<Pair<Int, Int>>(body.text)
                data.first shouldBe config.expectedMovedAttributeId
                data.second shouldBe config.expectedTargetAttributeId

                respond(
                    content = ByteReadChannel(
                        json.encodeToString(config.returnOperationResult)
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }

            KB_INFO -> {
                respond(
                    content = ByteReadChannel(
                        json.encodeToString(config.returnKBInfo)
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }

            CONDITION_HINTS -> {
                if (config.expectedCaseId != null) request.url.parameters["id"] shouldBe config.expectedCaseId.toString()
                respond(
                    content = ByteReadChannel(
                        json.encodeToString(config.returnConditionList)
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }


            else -> {
                error("Unhandled ${request.url.fullPath}")
            }
        }
    }
}

