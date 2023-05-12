package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.verify
import io.rippledown.constants.api.BUILD_RULE
import io.rippledown.constants.api.VERIFIED_INTERPRETATION_SAVED
import io.rippledown.model.Interpretation
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.diff.*
import kotlin.test.Test

class InterpManagementTest : OpenRDRServerTestBase() {

    @Test
    fun `should delegate saving an Interpretation to server application`() = testApplication {
        setup()
        val rdrCase = RDRCase("Case1")
        val diffListToReturn = DiffList(
            listOf(
                Unchanged("Go to Bondi Beach."),
                Addition("Bring your handboard."),
                Removal("Don't forget your towel."),
                Replacement("And have fun.", "And have lots of fun.")
            )
        )
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
    fun `should delegate building a rule to server application`() = testApplication {
        setup()
        val rdrCase = RDRCase("Case1")
        val diffListToReturn = DiffList(
            listOf(
                Unchanged("Go to Bondi Beach."),
                Addition("Bring your handboard."),
                Removal("Don't forget your towel."),
                Replacement("And have fun.", "And have lots of fun.")
            )
        )
        val viewableCase = ViewableCase(rdrCase)
        val interpretationToUseForRule = viewableCase.interpretation.apply {
            verifiedText = "Verified Text"
        }
        val interpretationToReturn = interpretationToUseForRule.apply {
            diffList = diffListToReturn
        }
        every { serverApplication.buildRule(interpretationToUseForRule) } returns interpretationToReturn

        val result = httpClient.post(BUILD_RULE) {
            contentType(ContentType.Application.Json)
            setBody(interpretationToUseForRule)
        }
        result.status shouldBe HttpStatusCode.OK
        result.body<Interpretation>() shouldBe interpretationToReturn
        verify { serverApplication.buildRule(interpretationToUseForRule) }
    }
}