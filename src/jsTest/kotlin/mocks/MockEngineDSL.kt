package mocks

import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.content.*
import io.ktor.utils.io.*
import io.rippledown.model.CasesInfo
import io.rippledown.model.Interpretation
import io.rippledown.model.OperationResult
import io.rippledown.model.RDRCase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun engine(block: EngineBuilder.() -> Unit) =
    EngineBuilder()
        .apply(block)
        .build()

class EngineBuilder {
    var returnCasesInfo = CasesInfo()
    var returnCase = RDRCase()
    var returnOperationResult = OperationResult()

    var expectedCaseId = ""
    var expectedInterpretation = Interpretation()

    private val json = Json {
        allowStructuredMapKeys = true
    }

    fun build() = MockEngine { request ->
        when (request.url.encodedPath) {
            "/api/waitingCasesInfo" -> {
                request.method shouldBe Get
                respond(
                    content = ByteReadChannel(
                        json.encodeToString(returnCasesInfo)
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }

            "/api/case" -> {
                request.method shouldBe Get
                request.url.parameters["id"] shouldBe expectedCaseId
                respond(
                    content = ByteReadChannel(
                        json.encodeToString(returnCase)
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }

            "/api/interpretationSubmitted" -> {
                request.method shouldBe Post
                val expectedBody = request.body as TextContent
                expectedBody.text shouldBe json.encodeToString(expectedInterpretation)

                respond(
                    content = ByteReadChannel(
                        json.encodeToString(returnOperationResult)
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

val defaultMock = engine {
    returnCasesInfo = CasesInfo()
}