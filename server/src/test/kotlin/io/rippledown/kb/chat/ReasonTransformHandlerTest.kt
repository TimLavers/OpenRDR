package io.rippledown.kb.chat

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.chat.Conversation.Companion.REASON_PARAMETER
import io.rippledown.chat.ReasonTransformation
import io.rippledown.chat.ReasonTransformer
import io.rippledown.constants.chat.EXEMPT_CORNERSTONE
import io.rippledown.kb.chat.ReasonTransformHandler.Companion.ALLOW_CORNERSTONE_CORRECTION
import io.rippledown.model.rule.CornerstoneStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ReasonTransformHandlerTest {
    private val reasonTransformer: ReasonTransformer = mockk()
    private val ruleService: RuleService = mockk()
    private val handler = ReasonTransformHandler(reasonTransformer, ruleService)

    @Test
    fun `should redirect an allow confirmation to the exempt cornerstone action when a review is pending`() = runTest {
        // Given a cornerstone review is pending
        every { ruleService.cornerstoneStatus() } returns CornerstoneStatus(numberOfCornerstones = 2)

        // When the model routes "allow" into the reason-transform function
        val result = handler.handle(mapOf(REASON_PARAMETER to "allow"))

        // Then the model is redirected to emit the ExemptCornerstone action and no transform is attempted
        result shouldBe ALLOW_CORNERSTONE_CORRECTION
        result shouldContain EXEMPT_CORNERSTONE
        coVerify(exactly = 0) { reasonTransformer.transform(any()) }
    }

    @Test
    fun `should treat allow as a normal reason when no cornerstone review is pending`() = runTest {
        // Given no cornerstone review is pending
        every { ruleService.cornerstoneStatus() } returns CornerstoneStatus(numberOfCornerstones = 0)
        coEvery { reasonTransformer.transform(any()) } returns ReasonTransformation(message = "I do not understand")

        // When
        handler.handle(mapOf(REASON_PARAMETER to "allow"))

        // Then it is passed to the transformer as usual
        coVerify { reasonTransformer.transform("allow") }
    }

    @Test
    fun `should transform a genuine reason even while a cornerstone review is pending`() = runTest {
        // Given a cornerstone review is pending
        every { ruleService.cornerstoneStatus() } returns CornerstoneStatus(numberOfCornerstones = 2)
        coEvery { reasonTransformer.transform(any()) } returns ReasonTransformation(
            reasonId = 1,
            message = "wave height > 0.5"
        )

        // When the user provides a genuine reason
        handler.handle(mapOf(REASON_PARAMETER to "wave height is more than 0.5"))

        // Then it is transformed rather than redirected
        coVerify { reasonTransformer.transform("wave height is more than 0.5") }
    }
}
