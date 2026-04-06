package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse
import io.rippledown.model.rule.CornerstoneStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class NextCornerstoneTest {
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
    fun `should select the next cornerstone case`() = runTest {
        //Given
        val action = NextCornerstone()
        val currentStatus = CornerstoneStatus(indexOfCornerstoneToReview = 0, numberOfCornerstones = 3)
        every { ruleService.cornerstoneStatus() } returns currentStatus

        val updatedStatus = CornerstoneStatus(indexOfCornerstoneToReview = 1, numberOfCornerstones = 3)
        every { ruleService.selectCornerstoneCase(1) } returns updatedStatus

        val responseFromModel = ChatResponse("showing next cornerstone case")
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify { ruleService.selectCornerstoneCase(1) }
        coVerify { ruleService.sendCornerstoneStatus() }
        coVerify { modelResponder.response(updatedStatus.summary()) }
        response shouldBe responseFromModel
    }

    @Test
    fun `should navigate from the second to the third cornerstone`() = runTest {
        //Given
        val action = NextCornerstone()
        val currentStatus = CornerstoneStatus(indexOfCornerstoneToReview = 1, numberOfCornerstones = 5)
        every { ruleService.cornerstoneStatus() } returns currentStatus

        val updatedStatus = CornerstoneStatus(indexOfCornerstoneToReview = 2, numberOfCornerstones = 5)
        every { ruleService.selectCornerstoneCase(2) } returns updatedStatus

        val responseFromModel = ChatResponse("next cornerstone")
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify { ruleService.selectCornerstoneCase(2) }
        coVerify { ruleService.sendCornerstoneStatus() }
        coVerify { modelResponder.response(updatedStatus.summary()) }
        response shouldBe responseFromModel
    }

    @Test
    fun `should send cornerstone status before responding to model`() = runTest {
        //Given
        val action = NextCornerstone()
        val currentStatus = CornerstoneStatus(indexOfCornerstoneToReview = 0, numberOfCornerstones = 2)
        every { ruleService.cornerstoneStatus() } returns currentStatus

        val updatedStatus = CornerstoneStatus(indexOfCornerstoneToReview = 1, numberOfCornerstones = 2)
        every { ruleService.selectCornerstoneCase(1) } returns updatedStatus

        val responseFromModel = ChatResponse("next")
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify(ordering = io.mockk.Ordering.ORDERED) {
            ruleService.sendCornerstoneStatus()
            modelResponder.response(updatedStatus.summary())
        }
    }

    @Test
    fun `should pass the updated cornerstone summary to the model`() = runTest {
        //Given
        val action = NextCornerstone()
        val currentStatus = CornerstoneStatus(indexOfCornerstoneToReview = 2, numberOfCornerstones = 10)
        every { ruleService.cornerstoneStatus() } returns currentStatus

        val updatedStatus = CornerstoneStatus(indexOfCornerstoneToReview = 3, numberOfCornerstones = 10)
        every { ruleService.selectCornerstoneCase(3) } returns updatedStatus

        val responseFromModel = ChatResponse("review this cornerstone")
        coEvery { modelResponder.response(updatedStatus.summary()) } returns responseFromModel

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then
        response shouldBe responseFromModel
    }
}
