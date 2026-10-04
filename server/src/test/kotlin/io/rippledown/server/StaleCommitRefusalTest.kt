package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.server.testing.*
import io.mockk.every
import io.rippledown.constants.api.COMMIT_RULE_SESSION
import io.rippledown.constants.api.START_RULE_SESSION
import io.rippledown.constants.server.KB_ID
import io.rippledown.constants.server.REFUSAL_HEADER
import io.rippledown.constants.server.REFUSAL_HELD
import io.rippledown.constants.server.REFUSAL_STALE
import io.rippledown.kb.lease.ProjectHeldException
import io.rippledown.model.StaleRuleSessionException
import io.rippledown.model.UserId
import io.rippledown.model.condition.RuleConditionList
import io.rippledown.model.diff.Addition
import io.rippledown.model.interpretationChangedMessage
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
import kotlin.test.Test

/**
 * The two kinds of 409 the server sends are told apart by a header, so that the
 * client can raise the right exception for each.
 * See documentation/design/concurrent_users_revalidation.md.
 */
class StaleCommitRefusalTest : OpenRDRServerTestBase() {

    @Test
    fun `a stale commit is refused with 409 and the stale refusal header`() = testApplication {
        // Given
        setupServer()
        val request = RuleRequest(1L, RuleConditionList())
        val message = interpretationChangedMessage("Case1")
        every { kbEndpoint.commitRuleSession(request, testUser) } throws StaleRuleSessionException(message)

        // When
        val result = httpClient.post(COMMIT_RULE_SESSION) {
            contentType(ContentType.Application.Json)
            setBody(request)
            parameter(KB_ID, kbId)
        }

        // Then
        result.status shouldBe Conflict
        result.headers[REFUSAL_HEADER] shouldBe REFUSAL_STALE
        result.bodyAsText() shouldBe message
    }

    @Test
    fun `a request refused because the KB is held carries the held refusal header`() = testApplication {
        // Given
        setupServer()
        every { kbSession.hold(testUser) } throws ProjectHeldException(kbName, UserId("alice"))

        // When
        val result = httpClient.post(START_RULE_SESSION) {
            contentType(ContentType.Application.Json)
            setBody(SessionStartRequest(1L, Addition("Go.")))
            parameter(KB_ID, kbId)
        }

        // Then
        result.status shouldBe Conflict
        result.headers[REFUSAL_HEADER] shouldBe REFUSAL_HELD
    }
}
