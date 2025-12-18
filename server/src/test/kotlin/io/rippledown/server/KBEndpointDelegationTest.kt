package io.rippledown.server

import io.mockk.mockk
import io.mockk.verify
import io.rippledown.kb.KB
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class KBEndpointDelegationTest {

    private lateinit var kb: KB

    private lateinit var endpoint: KBEndpoint

    @BeforeTest
    fun setup() {
        kb = mockk<KB>()
        endpoint = KBEndpoint(kb)
    }

    @Test
    fun `should delegate to the kb to cancel a rule session`() {
        // When
        endpoint.cancelRuleSession()

        // Then
        verify { kb.cancelRuleSession() }
    }

}