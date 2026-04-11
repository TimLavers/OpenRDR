package io.rippledown.server

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.kb.KBSession
import io.rippledown.kb.RuleSessionManager
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
        every { session.ruleSessionManager } returns rsm
        endpoint = KBEndpoint(session)
    }

    @Test
    fun `should delegate to the ruleSessionManager to cancel a rule session`() {
        // When
        endpoint.cancelRuleSession()

        // Then
        verify { rsm.cancelRuleSession() }
    }

}