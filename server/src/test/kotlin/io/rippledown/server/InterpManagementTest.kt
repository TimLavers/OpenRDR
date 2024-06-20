package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.verify
import io.rippledown.constants.api.*
import io.rippledown.constants.server.KB_ID
import io.rippledown.model.CaseId
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.createCase
import io.rippledown.model.diff.*
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
import io.rippledown.model.rule.UpdateCornerstoneRequest
import kotlin.test.Test

class InterpManagementTest : OpenRDRServerTestBase() {

    @Test
    fun `should delegate saving an Interpretation to server application`() = testApplication {
        setup()
        val rdrCase = RDRCase(CaseId(100, "Case1"))
        val viewableCase = ViewableCase(rdrCase)
        val interpretationToSave = viewableCase.viewableInterpretation.apply { verifiedText = "Verified text" }

        val diffs = DiffList(
            listOf(
                Unchanged("Go to Bondi Beach."),
                Addition("Bring your handboard."),
                Removal("Don't forget your towel."),
                Replacement("And have fun.", "And have lots of fun.")
            )
        )

        val interpretationToReturn = interpretationToSave.apply {
            diffList = diffs
        }
        val caseToReturn = viewableCase.apply {
            viewableInterpretation = interpretationToReturn
        }
        every { kbEndpoint.saveInterpretation(viewableCase) } returns caseToReturn

        val result = httpClient.post(VERIFIED_INTERPRETATION_SAVED) {
            parameter(KB_ID, kbId)
            contentType(ContentType.Application.Json)
            setBody(viewableCase)
        }
        result.status shouldBe OK
        result.body<ViewableCase>() shouldBe caseToReturn
        verify { kbEndpoint.saveInterpretation(viewableCase) }
    }

    @Test
    fun `should delegate starting a rule session to server application`() = testApplication {
        setup()
        val diff = Addition("Bring your handboard.")

        val caseId = 1L
        val sessionStartRequest = SessionStartRequest(caseId, diff)
        val cornerstoneStatus = CornerstoneStatus()
        every { kbEndpoint.startRuleSession(sessionStartRequest) } returns cornerstoneStatus

        val result = httpClient.post(START_RULE_SESSION) {
            parameter(KB_ID, kbId)
            contentType(ContentType.Application.Json)
            setBody(sessionStartRequest)
        }
        result.status shouldBe OK
        result.body<CornerstoneStatus>() shouldBe cornerstoneStatus
        verify { kbEndpoint.startRuleSession(sessionStartRequest) }
    }

    @Test
    fun `should delegate updating the cornerstone status of a rule session to server application`() = testApplication {
        setup()
        val request = UpdateCornerstoneRequest(CornerstoneStatus(), ConditionList())
        val cornerstoneStatus = CornerstoneStatus()
        every { kbEndpoint.updateCornerstone(request) } returns cornerstoneStatus

        val result = httpClient.post(UPDATE_CORNERSTONES) {
            parameter(KB_ID, kbId)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        result.status shouldBe OK
        result.body<CornerstoneStatus>() shouldBe cornerstoneStatus
        verify { kbEndpoint.updateCornerstone(request) }
    }

    @Test
    fun `should delegate exempting cornerstone to server application`() = testApplication {
        setup()
        val cornerstoneStatus = CornerstoneStatus()
        val updatedCornerstoneStatus = CornerstoneStatus(createCase("Bondi"), 42, 100)
        every { kbEndpoint.exemptCornerstone(cornerstoneStatus) } returns updatedCornerstoneStatus

        val result = httpClient.post(EXEMPT_CORNERSTONE) {
            parameter(KB_ID, kbId)
            contentType(ContentType.Application.Json)
            setBody(cornerstoneStatus)
        }
        result.status shouldBe OK
        result.body<CornerstoneStatus>() shouldBe updatedCornerstoneStatus
        verify { kbEndpoint.exemptCornerstone(cornerstoneStatus) }
    }

    @Test
    fun `should delegate building a rule to server application`() = testApplication {
        setup()

        val ruleRequest = RuleRequest(1)
        val viewableCase = createCase(CaseId(1, "Bondi"))
        every { kbEndpoint.commitRuleSession(ruleRequest) } returns viewableCase

        val result = httpClient.post(BUILD_RULE) {
            parameter(KB_ID, kbId)
            contentType(ContentType.Application.Json)
            setBody(ruleRequest)
        }
        result.status shouldBe OK
        result.body<ViewableCase>() shouldBe viewableCase
        verify { kbEndpoint.commitRuleSession(ruleRequest) }
    }

    @Test
    fun `should delegate selecting a cornerstone case to server application`() = testApplication {
        setup()

        val index = 42
        val cc = createCase("bondi")
        every { kbEndpoint.cornerstoneForIndex(index) } returns cc

        val result = httpClient.get(SELECT_CORNERSTONE) {
            parameter(KB_ID, kbId)
            parameter(INDEX_PARAMETER, index)
        }
        result.status shouldBe OK
        result.body<ViewableCase>() shouldBe cc
        result.status shouldBe OK
        verify { kbEndpoint.cornerstoneForIndex(index) }
    }
}