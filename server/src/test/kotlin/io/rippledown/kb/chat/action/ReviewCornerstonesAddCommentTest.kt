package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.toJsonString
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ReviewCornerstonesAddCommentTest : ActionTestBase() {
    @Test
    fun `should start a rule session to add a comment`() = runTest {
        //Given
        val action = ReviewCornerstonesAddComment(commentToAdd)
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 42, numberOfCornerstones = 84)
        coEvery {
            ruleService.startCornerstoneReviewSessionToAddComment(any(), commentToAdd)
        } returns ccStatus

        val responseFromModel = "There are 84 cornstone cases. Do you want to review them?"
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify { modelResponder.response(ccStatus.toJsonString<CornerstoneStatus>()) }
        response shouldBe responseFromModel
    }
}