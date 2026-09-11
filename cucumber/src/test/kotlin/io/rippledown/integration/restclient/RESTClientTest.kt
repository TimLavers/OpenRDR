package io.rippledown.integration.restclient

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.rippledown.constants.api.CREATE_KB
import io.rippledown.constants.api.DELETE_CASE_WITH_NAME
import io.rippledown.constants.server.KB_ID
import io.rippledown.main.Api
import io.rippledown.model.KBInfo
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class RESTClientTest {
    @ParameterizedTest
    @CsvSource("Thyroids, 1", "Thyroids, 2", "Research, 2")
    fun `case deletions use the KB created by this REST client`(kbName: String, caseCount: Int) {
        // Given
        val kb = KBInfo("selected_kb", kbName)
        val deletions = mutableListOf<Pair<String?, String?>>()
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                CREATE_KB -> respond(
                    Json.encodeToString(kb),
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )

                DELETE_CASE_WITH_NAME -> {
                    request.method shouldBe HttpMethod.Delete
                    deletions.add(request.url.parameters[KB_ID] to request.url.parameters["name"])
                    respond("")
                }

                else -> error("Unexpected request: ${request.url}")
            }
        }
        val api = Api(engine)
        try {
            val client = RESTClient(api)
            client.createKB(kbName)
            val names = (1..caseCount).map { "Case$it" }

            // When
            names.forEach(client::deleteProcessedCaseWithName)

            // Then
            deletions shouldBe names.map { kb.id to it }
        } finally {
            api.client.close()
        }
    }

    @Test
    fun `case deletion without an open KB fails before sending a request`() {
        // Given
        val api = Api(MockEngine { error("No request should be sent without an open KB") })
        try {
            val client = RESTClient(api)

            // When
            val failure = shouldThrow<IllegalStateException> { client.deleteProcessedCaseWithName("Case1") }

            // Then
            failure.message shouldBe "No knowledge base is open."
        } finally {
            api.client.close()
        }
    }
}
