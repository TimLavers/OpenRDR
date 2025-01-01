package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.verify
import io.rippledown.constants.api.CONDITION_FOR_EXPRESSION
import io.rippledown.constants.server.EXPRESSION
import io.rippledown.constants.server.KB_ID
import io.rippledown.model.Attribute
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.High
import io.rippledown.model.condition.episodic.signature.Current
import kotlin.test.Test

class RuleSessionsTest : OpenRDRServerTestBase() {

    @Test
    fun `should delegate requesting a condition for an expression to the server application`() = testApplication {
        setup()
        val expression = "elevated waves"
        val attributeNames = listOf("Sun, surf")
        val waves = Attribute(0, "Waves")
        val condition = EpisodicCondition(null, waves, High, Current, expression)
        every { kbEndpoint.conditionForExpression(any<String>(), any<List<String>>()) } returns condition

        val result = httpClient.get(CONDITION_FOR_EXPRESSION) {
            contentType(ContentType.Application.Json)
            parameter(EXPRESSION, expression)
            parameter(KB_ID, kbId)
            setBody(attributeNames)
        }
        result.status shouldBe OK
        result.body<Condition?>() shouldBe condition
        verify { kbEndpoint.conditionForExpression(expression, attributeNames) }
    }


}