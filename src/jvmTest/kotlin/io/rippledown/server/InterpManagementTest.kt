package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.verify
import io.rippledown.constants.api.BUILD_RULE
import io.rippledown.constants.api.START_RULE_SESSION
import io.rippledown.constants.api.VERIFIED_INTERPRETATION_SAVED
import io.rippledown.model.CaseId
import io.rippledown.model.Interpretation
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.diff.*
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
import kotlin.test.Test

class InterpManagementTest : OpenRDRServerTestBase() {

    @Test
    fun `should delegate saving an Interpretation to server application`() = testApplication {
        setup()
        val rdrCase = RDRCase(CaseId(100, "Case1"))
        val diffs = DiffList(
            listOf(
                Unchanged("Go to Bondi Beach."),
                Addition("Bring your handboard."),
                Removal("Don't forget your towel."),
                Replacement("And have fun.", "And have lots of fun.")
            )
        )
        val diffListToReturn = diffs
        val viewableCase = ViewableCase(rdrCase)
        val interpretationToSave = viewableCase.interpretation.apply {
            verifiedText = "Verified Text"
        }
        val interpretationToReturn = interpretationToSave.apply {
            diffList = diffListToReturn
        }
        every { serverApplication.saveInterpretation(interpretationToSave) } returns interpretationToReturn

        val result = httpClient.post(VERIFIED_INTERPRETATION_SAVED) {
            contentType(ContentType.Application.Json)
            setBody(interpretationToSave)
        }
        result.status shouldBe HttpStatusCode.OK
        result.body<Interpretation>() shouldBe interpretationToReturn
        verify { serverApplication.saveInterpretation(interpretationToSave) }
    }

    @Test
    fun `should delegate starting a rule session to server application`() = testApplication {
        setup()
        val diff = Addition("Bring your handboard.")

        val caseId = 1L
        val sessionStartRequest = SessionStartRequest(caseId, diff)
        val cornerstoneStatus = CornerstoneStatus()
        every { serverApplication.startRuleSession(sessionStartRequest) } returns cornerstoneStatus

        val result = httpClient.post(START_RULE_SESSION) {
            contentType(ContentType.Application.Json)
            setBody(sessionStartRequest)
        }
        result.status shouldBe HttpStatusCode.OK
        result.body<CornerstoneStatus>() shouldBe cornerstoneStatus
        verify { serverApplication.startRuleSession(sessionStartRequest) }
    }

    @Test
    fun `should delegate building a rule to server application`() = testApplication {
        setup()
        val diffList = DiffList(
            diffs = listOf(
                Unchanged("Go to Bondi Beach."),
                Addition("Bring your handboard."),
                Removal("Don't forget your towel."),
                Replacement("And have fun.", "And have lots of fun.")
            ),
            selected = 2
        )

        val ruleRequest = RuleRequest(1, diffList = diffList)
        val interp = Interpretation()
        every { serverApplication.commitRuleSession(ruleRequest) } returns interp

        val result = httpClient.post(BUILD_RULE) {
            contentType(ContentType.Application.Json)
            setBody(ruleRequest)
        }
        result.status shouldBe HttpStatusCode.OK
        result.body<Interpretation>() shouldBe interp
        verify { serverApplication.commitRuleSession(ruleRequest) }
    }
}