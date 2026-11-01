package io.rippledown.integration.restclient

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.rippledown.constants.api.*
import io.rippledown.constants.server.KB_ID
import io.rippledown.constants.server.RESET
import io.rippledown.main.Api
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.KBInfo
import io.rippledown.model.KnowledgeBaseHeldException
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatContextInfo
import io.rippledown.sample.SampleKB
import io.rippledown.toJsonString
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class RESTClientTest {

    @Test
    fun `case checks in chat context read the imported KB without changing the conversation`() {
        // Given a REST client still pointing at the original KB after the UI re-imported it.
        val original = KBInfo("original", "Thyroids")
        val imported = KBInfo("imported", "Thyroids")
        val caseId = CaseId(7L, "Export01")
        val view = ViewableCase(RDRCase(caseId))
        val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                CREATE_KB -> respond(Json.encodeToString(original), headers = jsonHeaders)
                CHAT_CONTEXT -> respond(Json.encodeToString(ChatContextInfo(imported, caseId.id)), headers = jsonHeaders)
                WAITING_CASES -> {
                    request.url.parameters[KB_ID] shouldBe imported.id
                    respond(Json.encodeToString(CasesInfo(caseIds = listOf(caseId))), headers = jsonHeaders)
                }
                CASE -> {
                    request.url.parameters[KB_ID] shouldBe imported.id
                    respond(view.toJsonString(), headers = jsonHeaders)
                }
                else -> error("Unexpected request: ${request.url}")
            }
        }
        val client = RESTClient(Api(engine))
        try {
            client.createKB(original.name)

            // When
            val result = client.getProcessedCaseInChatContext(caseId.name)

            // Then the assertion reads the new KB using only GET requests.
            result shouldBe view
            engine.requestHistory.drop(1).map { it.method to it.url.encodedPath } shouldBe listOf(
                HttpMethod.Get to CHAT_CONTEXT,
                HttpMethod.Get to WAITING_CASES,
                HttpMethod.Get to CASE
            )
        } finally {
            client.close()
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `case checks cannot fall back to an old KB when chat has none`(conversationExists: Boolean) {
        // Given an old local selection and either no conversation or a conversation without a KB.
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                CREATE_KB -> respond(
                    Json.encodeToString(KBInfo("old", "Thyroids")),
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                CHAT_CONTEXT -> if (conversationExists) respond(
                    Json.encodeToString(ChatContextInfo()),
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                ) else respond("", HttpStatusCode.NoContent)
                else -> error("Unexpected request: ${request.url}")
            }
        }
        val client = RESTClient(Api(engine))
        try {
            client.createKB("Thyroids")

            // When
            val error = shouldThrow<IllegalStateException> { client.getProcessedCaseInChatContext("Export01") }

            // Then no case request is sent to the old KB.
            error.message shouldBe "No knowledge base is open in the chat context."
            engine.requestHistory.map { it.url.encodedPath } shouldBe listOf(CREATE_KB, CHAT_CONTEXT)
        } finally {
            client.close()
        }
    }

    @Test
    fun `successful reset clears the wrapped API selection`() = runBlocking<Unit> {
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
    fun `unsuccessful reset fails without clearing selection`(status: Int) = runBlocking<Unit> {
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
            if (status == HttpStatusCode.Conflict.value) {
                shouldThrow<KnowledgeBaseHeldException> { client.reset() }
            } else {
                shouldThrow<IllegalStateException> { client.reset() }
            }

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
