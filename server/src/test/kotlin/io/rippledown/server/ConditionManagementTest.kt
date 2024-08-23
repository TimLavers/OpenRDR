package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.verify
import io.rippledown.constants.api.CONDITION_HINTS
import io.rippledown.constants.api.GET_OR_CREATE_CONDITION
import io.rippledown.constants.server.KB_ID
import io.rippledown.model.Attribute
import io.rippledown.model.condition.*
import io.rippledown.model.condition.edit.FixedSuggestedCondition
import kotlinx.serialization.json.Json
import kotlin.test.Test

class ConditionManagementTest: OpenRDRServerTestBase() {
    @Test
    fun getOrCreateCondition() = testApplication {
        setup()
        val glucose = Attribute(33, "Glucose")
        val toReturn = isHigh(54, glucose)
        val template = isLow(null, glucose)
        every { kbEndpoint.getOrCreateCondition(template) } returns toReturn
        val data = Json.encodeToJsonElement(Condition.serializer(), template)
        val result = httpClient.post(GET_OR_CREATE_CONDITION) {
            parameter(KB_ID, kbId)
            contentType(ContentType.Application.Json)
            setBody(data)
        }
        result.status shouldBe HttpStatusCode.OK
        result.body<Condition>() shouldBe toReturn
        verify { kbEndpoint.getOrCreateCondition(template) }
    }

    @Test
    fun `should delegate generating condition hints to server application`() = testApplication {
        setup()
        val caseId = 42L
        val conditionList = ConditionList(
            listOf(
                FixedSuggestedCondition(isNormal(1, Attribute(1, "WaveHeight"))),
                FixedSuggestedCondition(isLow(2, Attribute(2, "SeaTemp")))
            )
        )
        every { kbEndpoint.conditionHintsForCase(caseId) } returns conditionList

        val result = httpClient.get(CONDITION_HINTS) {
            parameter(KB_ID, kbId)
            parameter("id", caseId)
        }

        result.status shouldBe HttpStatusCode.OK
        result.body<ConditionList>() shouldBe conditionList
        verify { kbEndpoint.conditionHintsForCase(caseId) }
    }
 }