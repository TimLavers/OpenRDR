package io.rippledown.main

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.rippledown.constants.api.CHAT_CONTEXT
import io.rippledown.constants.api.CORNERSTONE_STATUS
import io.rippledown.constants.api.SELECT_KB
import io.rippledown.constants.api.START_CONVERSATION
import io.rippledown.model.KBInfo
import io.rippledown.model.chat.ChatContextInfo
import io.rippledown.model.chat.ChatResponse
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.toJsonString
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ApiRecoverySnapshotTest {
    private val kb = KBInfo("clinic", "Clinic")
    private val other = KBInfo("other", "Other")
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun `starting a new conversation supersedes an in flight context snapshot`() = runTest {
        // Given
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val api = Api(MockEngine { request ->
            when (request.url.encodedPath) {
                SELECT_KB -> respond(kb.toJsonString(), headers = jsonHeaders)
                START_CONVERSATION -> respond(ChatResponse("").toJsonString(), headers = jsonHeaders)
                CHAT_CONTEXT -> {
                    started.complete(Unit)
                    release.await()
                    respond(ChatContextInfo(other, 1).toJsonString(), headers = jsonHeaders)
                }

                else -> error("Unexpected request: ${request.url}")
            }
        })
        try {
            api.selectKB(kb.id)
            val snapshot = async { api.chatContext() }
            started.await()

            // When
            api.startConversation(kb.id, 2)
            release.complete(Unit)
            snapshot.await()

            // Then
            api.kbInfo().id shouldBe kb.id
        } finally {
            api.client.close()
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `a late chat context snapshot cannot overwrite a newer selection or closure`(closed: Boolean) = runTest {
        // Given
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val api = Api(MockEngine { request ->
            when (request.url.encodedPath) {
                CHAT_CONTEXT -> {
                    started.complete(Unit)
                    release.await()
                    respond(ChatContextInfo(kb, 1).toJsonString(), headers = jsonHeaders)
                }

                SELECT_KB -> respond(other.toJsonString(), headers = jsonHeaders)
                else -> error("Unexpected request: ${request.url}")
            }
        })
        try {
            val snapshot = async { api.chatContext() }
            started.await()

            // When
            if (closed) api.clearKnowledgeBaseSelection() else api.selectKB(other.id)
            release.complete(Unit)
            snapshot.await()

            // Then
            if (closed) shouldThrow<IllegalStateException> { api.kbInfo() }
            else api.kbInfo().id shouldBe other.id
        } finally {
            api.client.close()
        }
    }

    @Test
    fun `a missing conversation clears the API selection`() = runTest {
        // Given
        val api = Api(MockEngine { request ->
            if (request.url.encodedPath == SELECT_KB) respond(kb.toJsonString(), headers = jsonHeaders)
            else respond("", HttpStatusCode.NoContent)
        })
        try {
            api.selectKB(kb.id)

            // When
            api.chatContext() shouldBe null

            // Then
            shouldThrow<IllegalStateException> { api.kbInfo() }
        } finally {
            api.client.close()
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `session snapshot decodes an active session or no content`(active: Boolean) = runTest {
        // Given
        val status = CornerstoneStatus()
        val api = Api(MockEngine { request ->
            when (request.url.encodedPath) {
                SELECT_KB -> respond(kb.toJsonString(), headers = jsonHeaders)
                CORNERSTONE_STATUS -> if (active) respond(status.toJsonString(), headers = jsonHeaders)
                else respond("", HttpStatusCode.NoContent)

                else -> error("Unexpected request: ${request.url}")
            }
        })
        try {
            api.selectKB(kb.id)

            // When
            val snapshot = api.cornerstoneStatus()

            // Then
            snapshot shouldBe if (active) status else null
        } finally {
            api.client.close()
        }
    }
}
