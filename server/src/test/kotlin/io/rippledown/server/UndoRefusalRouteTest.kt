package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.request.delete
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.rippledown.constants.api.LAST_RULE_DESCRIPTION
import io.rippledown.constants.server.KB_ID
import io.rippledown.constants.server.REFUSAL_HEADER
import io.rippledown.constants.server.REFUSAL_STALE
import io.rippledown.kb.UndoRuleRefusedException
import org.junit.jupiter.api.Test

class UndoRefusalRouteTest : OpenRDRServerTestBase() {
    @Test
    fun `a stale undo returns a conflict and the server's explanation`() = testApplication {
        // Given
        setupServer()
        val message = "The most recent rule changed since it was shown. New rule. Undo it?"
        every { kbEndpoint.undoLastRule(testUser) } throws UndoRuleRefusedException(message)

        // When
        val result = httpClient.delete(LAST_RULE_DESCRIPTION) { parameter(KB_ID, kbId) }

        // Then
        result.status shouldBe Conflict
        result.headers[REFUSAL_HEADER] shouldBe REFUSAL_STALE
        result.bodyAsText() shouldBe message
    }
}
