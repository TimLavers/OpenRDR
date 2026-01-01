package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.toJsonString
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

        val responseFromModel = "report change to cornerstone case allowed"
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify { modelResponder.response(ccStatus.toJsonString<CornerstoneStatus>()) }
        coVerify { ruleService.sendCornerstoneStatus() }
        response shouldBe responseFromModel
    }
}