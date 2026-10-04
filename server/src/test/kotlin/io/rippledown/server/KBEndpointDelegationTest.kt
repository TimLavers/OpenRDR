package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.kb.KBSession
import io.rippledown.kb.RuleSessionManager
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.UpdateCornerstoneRequest
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class KBEndpointDelegationTest {

    private lateinit var rsm: RuleSessionManager
    private lateinit var session: KBSession
    private lateinit var endpoint: KBEndpoint

    @BeforeTest
    fun setup() {
        rsm = mockk<RuleSessionManager>()
        session = mockk<KBSession>()
        every { session.ruleSessionManagerFor(TEST_USER) } returns rsm
        every { session.locked(any<() -> Any?>()) } answers { firstArg<() -> Any?>()() }
        endpoint = KBEndpoint(session)
    }

    @Test
    fun `should delegate to the ruleSessionManager to cancel a rule session, under the KB lock`() {
        // When
        endpoint.cancelRuleSession(TEST_USER)

        // Then
        verify { rsm.cancelRuleSession() }
        verify(exactly = 1) { session.locked(any<() -> Any?>()) }
    }

    @Test
    fun `should delegate to the user's ruleSessionManager to update the cornerstone status, under the KB lock`() {
        // Given
        val request = mockk<UpdateCornerstoneRequest>()
        val status = mockk<CornerstoneStatus>()
        every { rsm.updateCornerstone(request) } returns status

        // When
        val result = endpoint.updateCornerstone(request, TEST_USER)

        // Then
        result shouldBe status
        verify(exactly = 1) { session.locked(any<() -> Any?>()) }
    }

    @Test
    fun `should delegate to the user's ruleSessionManager to exempt a cornerstone, under the KB lock`() {
        // Given
        val status = mockk<CornerstoneStatus>()
        every { rsm.exemptCornerstone(3) } returns status

        // When
        val result = endpoint.exemptCornerstone(3, TEST_USER)

        // Then
        result shouldBe status
        verify(exactly = 1) { session.locked(any<() -> Any?>()) }
    }

}