package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.testing.*
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import io.rippledown.constants.api.BUILD_RULE
import io.rippledown.constants.api.CONDITION_FOR_EXPRESSION
import io.rippledown.constants.server.EXPRESSION
import io.rippledown.constants.server.KB_ID
import io.rippledown.model.Attribute
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.High
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Removal
import io.rippledown.model.diff.Replacement
import io.rippledown.model.rule.BuildRuleRequest
import kotlin.test.Test

class RuleSessionsTest : OpenRDRServerTestBase() {

    @Test
    fun `should delegate requesting a condition for an expression to the server application`() = testApplication {
        setupServer()
        val expression = "elevated waves"
        val waves = Attribute(0, "Waves")
        val condition = EpisodicCondition(null, waves, High, Current, expression)
        every { kbEndpoint.conditionForExpression(any<String>()) } returns ConditionParsingResult(
            condition
        )

        val result = httpClient.get(CONDITION_FOR_EXPRESSION) {
            contentType(ContentType.Application.Json)
            parameter(EXPRESSION, expression)
            parameter(KB_ID, kbId)
        }
        result.status shouldBe OK
        result.body<ConditionParsingResult>().condition shouldBe condition
        verify { kbEndpoint.conditionForExpression(expression) }
    }

    @Test
    fun `should delegate build rule request to the server application`() = testApplication {
        setupServer()
        val request = BuildRuleRequest(
            caseName = "Case1",
            diff = Addition("Glucose ok."),
            conditions = listOf("Glucose <= 1.5")
        )
        every { kbEndpoint.buildRule(request) } returns Unit
        every { kbEndpoint.waitingCasesInfo() } returns CasesInfo()

        val result = httpClient.post(BUILD_RULE) {
            contentType(ContentType.Application.Json)
            setBody(request)
            parameter(KB_ID, kbId)
        }

        result.status shouldBe OK
        verify { kbEndpoint.buildRule(request) }
    }

    @Test
    fun `should push CasesInfo via WebSocket after building a rule`() = testApplication {
        setupServer()
        val request = BuildRuleRequest(
            caseName = "Case1",
            diff = Addition("Go to Bondi."),
            conditions = listOf("TSH >= 0.6")
        )
        val updatedCasesInfo = CasesInfo(listOf(CaseId(id = 1, name = "Case1")), kbName = "TestKB")
        every { kbEndpoint.buildRule(request) } returns Unit
        every { kbEndpoint.waitingCasesInfo() } returns updatedCasesInfo

        httpClient.post(BUILD_RULE) {
            contentType(ContentType.Application.Json)
            setBody(request)
            parameter(KB_ID, kbId)
        }

        coVerify { webSocketManager.sendCasesInfo(updatedCasesInfo) }
    }

    @Test
    fun `should build rule with a Removal diff`() = testApplication {
        setupServer()
        val request = BuildRuleRequest(
            caseName = "Case2",
            diff = Removal("Outdated comment."),
            conditions = listOf("TSH is normal")
        )
        every { kbEndpoint.buildRule(request) } returns Unit
        every { kbEndpoint.waitingCasesInfo() } returns CasesInfo()

        val result = httpClient.post(BUILD_RULE) {
            contentType(ContentType.Application.Json)
            setBody(request)
            parameter(KB_ID, kbId)
        }

        result.status shouldBe OK
        verify { kbEndpoint.buildRule(request) }
    }

    @Test
    fun `should build rule with a Replacement diff`() = testApplication {
        setupServer()
        val request = BuildRuleRequest(
            caseName = "Case3",
            diff = Replacement("Old comment.", "New comment."),
            conditions = listOf("Free T4 is high")
        )
        every { kbEndpoint.buildRule(request) } returns Unit
        every { kbEndpoint.waitingCasesInfo() } returns CasesInfo()

        val result = httpClient.post(BUILD_RULE) {
            contentType(ContentType.Application.Json)
            setBody(request)
            parameter(KB_ID, kbId)
        }

        result.status shouldBe OK
        verify { kbEndpoint.buildRule(request) }
    }

    @Test
    fun `should build rule with empty conditions list`() = testApplication {
        setupServer()
        val request = BuildRuleRequest(
            caseName = "Case1",
            diff = Addition("Always applies."),
            conditions = emptyList()
        )
        every { kbEndpoint.buildRule(request) } returns Unit
        every { kbEndpoint.waitingCasesInfo() } returns CasesInfo()

        val result = httpClient.post(BUILD_RULE) {
            contentType(ContentType.Application.Json)
            setBody(request)
            parameter(KB_ID, kbId)
        }

        result.status shouldBe OK
        verify { kbEndpoint.buildRule(request) }
    }

    @Test
    fun `should build rule with multiple conditions`() = testApplication {
        setupServer()
        val request = BuildRuleRequest(
            caseName = "Case1",
            diff = Addition("Complex rule."),
            conditions = listOf("TSH >= 0.6", "Free T4 is high", "Age > 50")
        )
        every { kbEndpoint.buildRule(request) } returns Unit
        every { kbEndpoint.waitingCasesInfo() } returns CasesInfo()

        val result = httpClient.post(BUILD_RULE) {
            contentType(ContentType.Application.Json)
            setBody(request)
            parameter(KB_ID, kbId)
        }

        result.status shouldBe OK
        verify { kbEndpoint.buildRule(request) }
    }

    @Test
    fun `should return InternalServerError when buildRule throws an exception`() = testApplication {
        setupServer()
        val request = BuildRuleRequest(
            caseName = "NonExistentCase",
            diff = Addition("Some comment."),
            conditions = listOf("TSH >= 0.6")
        )
        every { kbEndpoint.buildRule(request) } throws Exception("Case not found")

        val result = httpClient.post(BUILD_RULE) {
            contentType(ContentType.Application.Json)
            setBody(request)
            parameter(KB_ID, kbId)
        }

        result.status shouldBe HttpStatusCode.InternalServerError
    }

    @Test
    fun `should return InternalServerError when KB_ID is missing for build rule`() = testApplication {
        setupServer()
        val request = BuildRuleRequest(
            caseName = "Case1",
            diff = Addition("Comment."),
            conditions = listOf("TSH >= 0.6")
        )

        val result = httpClient.post(BUILD_RULE) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        result.status shouldBe HttpStatusCode.InternalServerError
    }

}