package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse
import io.rippledown.model.rule.CornerstoneStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class ExemptCornerstoneTest {
    private lateinit var ruleService: RuleService
    private lateinit var currentCase: ViewableCase
    private lateinit var modelResponder: ModelResponder

    @BeforeTest
    fun setup() {
        ruleService = mockk()
        currentCase = mockk()
        modelResponder = mockk()
    }

    @Test
    fun `should allow the change to the report of the cornerstone case`() = runTest {
        //Given
        val action = ExemptCornerstone()
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 42, numberOfCornerstones = 84)
        coEvery { ruleService.exemptCornerstoneCase() } returns ccStatus

        val responseFromModel = ChatResponse("report change to cornerstone case allowed")
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify { modelResponder.response(ccStatus.summary()) }
        coVerify { ruleService.sendCornerstoneStatus() }
        response shouldBe responseFromModel
    }

    @Test
    fun `should tell the model to commit the rule when no cornerstones remain`() = runTest {
        // Given: the user has just allowed the change to the last cornerstone
        // case, so the rule engine reports Total = 0. Without an explicit
        // directive in the same turn, the model has been observed (e.g. on
        // the chat/Show cornerstones.feature 'allow a change to the report
        // of a cornerstone case' scenario) to fall back into "Here are some
        // suggestions" instead of committing the rule, leaving the cuke
        // harness waiting for a "Done" that never arrives.
        val action = ExemptCornerstone()
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = -1, numberOfCornerstones = 0)
        coEvery { ruleService.exemptCornerstoneCase() } returns ccStatus

        val responseFromModel = ChatResponse("rule committed")
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        // When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        // Then: the message must include both the bare summary AND a clear
        // CommitRule directive so the model has no excuse to ask for more
        // reasons.
        coVerify {
            modelResponder.response(
                match<String> { msg ->
                    msg.contains(ccStatus.summary()) &&
                            msg.contains("CommitRule") &&
                            msg.contains("All cornerstone cases have been reviewed")
                }
            )
        }
        coVerify { ruleService.sendCornerstoneStatus() }
        response shouldBe responseFromModel
    }
}