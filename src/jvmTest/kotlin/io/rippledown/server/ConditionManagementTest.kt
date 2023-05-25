package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.verify
import io.rippledown.constants.api.CONDITION_HINTS
import io.rippledown.model.Attribute
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.IsLow
import io.rippledown.model.condition.IsNormal
import kotlin.test.Test

class ConditionManagementTest : OpenRDRServerTestBase() {

    @Test
    fun `should delegate generating condition hints to server application`() = testApplication {
        setup()
        val caseId = "Bronte"
        val conditionList = ConditionList(
            listOf(
                IsNormal(Attribute("WaveHeight")),
                IsLow(Attribute("SeaTemp"))
            )
        )
        every { serverApplication.conditionHintsForCase(caseId) } returns conditionList

        val result = httpClient.get(CONDITION_HINTS) {
            parameter("id", caseId)
        }

        result.status shouldBe HttpStatusCode.OK
        result.body<ConditionList>() shouldBe conditionList
        verify { serverApplication.conditionHintsForCase(caseId) }
    }

}