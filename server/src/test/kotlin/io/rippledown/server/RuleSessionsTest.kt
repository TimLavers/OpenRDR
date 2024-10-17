package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.verify
import io.rippledown.constants.api.TIP_FOR_EXPRESSION
import io.rippledown.constants.server.ATTRIBUTE_NAMES
import io.rippledown.constants.server.EXPRESSION
import io.rippledown.constants.server.KB_ID
import kotlin.test.Test

class RuleSessionsTest : OpenRDRServerTestBase() {

    @Test
    fun `should delegate requesting a condition tip to the server application`() = testApplication {
        setup()
        val tip = "tip"
        val expression = "elevated waves"
        val attributeNames = "Sun, surf"
        every { kbEndpoint.tipForExpression(any<String>(), any<String>()) } returns tip

        val result = httpClient.get(TIP_FOR_EXPRESSION) {
            parameter(EXPRESSION, expression)
            parameter(ATTRIBUTE_NAMES, attributeNames)
            parameter(KB_ID, kbId)
        }
        result.status shouldBe OK
        result.body<String>() shouldBe tip
        verify { kbEndpoint.tipForExpression(expression, attributeNames) }
    }


}