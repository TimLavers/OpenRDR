package io.rippledown.integration.restclient

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.rippledown.constants.api.*
import io.rippledown.constants.server.KB_ID
import io.rippledown.constants.server.RESET
import io.rippledown.main.Api
import io.rippledown.model.KBInfo
import io.rippledown.sample.SampleKB
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class RESTClientTest {

    @Test
    fun `successful reset clears the wrapped API selection`() = runBlocking {
        // Given
        val kb = KBInfo("old", "Old")
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                CREATE_KB -> respond(
                    Json.encodeToString(kb),
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )

                RESET -> respond("OK")
                else -> error("Unexpected request: ${request.url}")
            }
        }
        val api = Api(engine)
        val client = RESTClient(api)
        try {
            client.createKB("Old")

            // When
            client.reset()

            // Then
            shouldThrow<IllegalStateException> { api.kbInfo() }
            engine.requestHistory.last().method shouldBe HttpMethod.Post
        } finally {
            client.close()
        }
    }

    @ParameterizedTest
    @CsvSource("409", "500", "404")
    fun `unsuccessful reset fails without clearing selection`(status: Int) = runBlocking {
        // Given
        val kb = KBInfo("old", "Old")
        val api = Api(MockEngine { request ->
            if (request.url.encodedPath == RESET) respond("Cannot reset", status = HttpStatusCode.fromValue(status))
            else respond(Json.encodeToString(kb), headers = headersOf(HttpHeaders.ContentType, "application/json"))
        })
        val client = RESTClient(api)
        try {
            client.createKB("Old")

            // When
            shouldThrow<IllegalStateException> { client.reset() }

            // Then
            api.kbInfo() shouldBe kb
        } finally {
            client.close()
        }
    }

    @Test
    fun `sample setup selects the created KB for subsequent requests`() {
        // Given
        val kb = KBInfo("sample_id", "TSHCases")
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                CREATE_KB_FROM_SAMPLE -> respond(
                    Json.encodeToString(kb),
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )

                DELETE_CASE_WITH_NAME -> {
                    request.url.parameters[KB_ID] shouldBe kb.id
                    respond("")
                }

                else -> error("Unexpected request: ${request.url}")
            }
        }
        val api = Api(engine)
        try {
            val client = RESTClient(api)

            // When
            client.createKBFromSample("TSHCases", SampleKB.TSH_CASES)
            client.deleteProcessedCaseWithName("Case1")

            // Then
            engine.requestHistory.map { it.url.encodedPath } shouldBe listOf(
                CREATE_KB_FROM_SAMPLE,
                DELETE_CASE_WITH_NAME
            )
        } finally {
            api.client.close()
        }
    }

    @Test
    fun `description checks read the named KB without selecting another KB`() {
        // Given
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                KB_LIST -> respond(
                    Json.encodeToString(listOf(KBInfo("a", "Irons"), KBInfo("b", "Glucose"))),
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )

                KB_DESCRIPTION -> {
                    request.url.parameters[KB_ID] shouldBe "b"
                    respond("Glucose description")
                }

                else -> error("Unexpected request: ${request.url}")
            }
        }
        val api = Api(engine)
        try {
            // When
            val description = RESTClient(api).kbDescription("Glucose")

            // Then
            description shouldBe "Glucose description"
        } finally {
            api.client.close()
        }
    }
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
