package mocks

import createCase
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.*
import io.rippledown.constants.api.*
import io.rippledown.model.*
import io.rippledown.model.caseview.ViewableCase
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun mock(config: EngineConfig) = EngineBuilder(config).build()

val defaultMock = mock(EngineConfig())

fun config(block: EngineConfig.() -> Unit) = EngineConfig().apply(block)

class EngineConfig {
    var returnCasesInfo: CasesInfo = CasesInfo(emptyList())
    var returnCase: ViewableCase = createCase("The Case")
    var returnOperationResult: OperationResult = OperationResult()
    var returnInterpretation: Interpretation = Interpretation()

    var expectedCaseId = ""
    var expectedInterpretation: Interpretation? = null

    var expectedMovedAttribute: Attribute? = null
    var expectedTargetAttribute: Attribute? = null

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
                if (config.expectedCaseId.isNotBlank()) request.url.parameters["id"] shouldBe config.expectedCaseId
                respond(
                    content = ByteReadChannel(
                        json.encodeToString(config.returnCase)
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }

            INTERPRETATION_SUBMITTED -> {
                val body = request.body as TextContent
                val bodyAsInterpretation = Json.decodeFromString(Interpretation.serializer(), body.text)
                if (config.expectedInterpretation != null) {
                    bodyAsInterpretation shouldBe config.expectedInterpretation
                }

                respond(
                    content = ByteReadChannel(
                        json.encodeToString(config.returnOperationResult)
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
                        json.encodeToString(config.returnInterpretation)
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }

            MOVE_ATTRIBUTE_JUST_BELOW_OTHER -> {
                val body = request.body as TextContent
                val data = Json.decodeFromString<Pair<Attribute, Attribute>>(body.text)
                data.first shouldBe config.expectedMovedAttribute
                data.second shouldBe config.expectedTargetAttribute

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

            else -> {
                error("Unhandled ${request.url.fullPath}")
            }
        }
    }
}

