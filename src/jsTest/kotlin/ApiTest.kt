import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.*
import io.rippledown.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ApiTest {

    @Test
    fun getCaseTest() = runTest {
        val mockEngine = MockEngine { request ->
            when (request.url.fullPath) {
                "/api/case?id=id1" -> {
                    respond(
                        content = ByteReadChannel(
                            """
                            {
                                "name": "Case A"
                            }
                        """.trimIndent()
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

        val apiClient = ApiClient(mockEngine)
        apiClient.getCase("id1") shouldBe RDRCase("Case A", mapOf())
    }

    @Test
    fun waitingForCasesInfoTest() = runTest {
        val mockEngine = MockEngine { request ->
            when (request.url.fullPath) {
                "/api/waitingCasesInfo" -> {
                    respond(
                        content = ByteReadChannel(
                            """{
                            "caseIds": [{ "id": "id1",
                                          "name": "Case A"
                                      },
                                      { "id": "id2",
                                        "name": "Case B"
                                      }]
                            "resourcePath": "path"
                        }""".trimIndent()
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

        val apiClient = ApiClient(mockEngine)
        apiClient.waitingCasesInfo() shouldBe CasesInfo(
            listOf(CaseId("id1", "Case A"), CaseId("id2", "Case B")),
            "path"
        )
    }

    @Test
    fun saveInterpretationTest() = runTest {
        val mockEngine = MockEngine { request ->
            request.method shouldBe HttpMethod.Post
            val expectedBody = request.body as TextContent
            expectedBody.text shouldBe
                    """{"caseId":{"id":"id1","name":"Case A"},"text":"report text"}""".trimIndent()

            when (request.url.fullPath) {
                "/api/interpretationSubmitted" -> {
                    respond(
                        content = ByteReadChannel(
                            """
                            {
                                "message": "saved interpretation for case A"
                            }
                        """.trimIndent()
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

        val apiClient = ApiClient(mockEngine)
        val interpretation = Interpretation(CaseId("id1", "Case A"), "report text")
        apiClient.saveInterpretation(interpretation) shouldBe OperationResult("saved interpretation for case A")
    }


}

