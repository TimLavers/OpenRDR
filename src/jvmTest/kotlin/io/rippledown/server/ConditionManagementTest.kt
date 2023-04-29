package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.verify
import io.rippledown.model.Attribute
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.IsHigh
import io.rippledown.model.condition.IsLow
import io.rippledown.server.routes.GET_OR_CREATE_CONDITION
import kotlin.test.Test

class ConditionManagementTest: OpenRDRServerTestBase() {
    @Test
    fun getOrCreateCondition() = testApplication {
        setupWithMock()
        val glucose = Attribute("Glucose", 33)
        val toReturn = IsHigh(54, glucose)
        val template = IsLow(null, glucose)
        every { serverApplicationMock.getOrCreateCondition(template) } returns toReturn
        val result = httpClient.post(GET_OR_CREATE_CONDITION) {
            setBody(template)
        }
        result.status shouldBe HttpStatusCode.OK
        result.body<Condition>() shouldBe toReturn
        verify { serverApplicationMock.getOrCreateCondition(template) }
    }
 }