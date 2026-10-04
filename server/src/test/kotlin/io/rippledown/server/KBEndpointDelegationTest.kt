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

}