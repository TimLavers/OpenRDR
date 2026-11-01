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
import io.rippledown.constants.chat.COMMIT_RULE
import io.rippledown.constants.chat.EXEMPT_CORNERSTONE
import io.rippledown.constants.chat.EXPLAIN_AI_EVALUATION
import io.rippledown.fromJsonString
import io.rippledown.kb.chat.ReasonTransformHandler.Companion.ALLOW_CORNERSTONE_CORRECTION
import io.rippledown.kb.chat.ReasonTransformHandler.Companion.DECLINE_CORRECTION
import io.rippledown.kb.chat.ReasonTransformHandler.Companion.declineWithCornerstonesToReviewCorrection
import io.rippledown.kb.chat.ReasonTransformHandler.Companion.explainRequestCorrection
import io.rippledown.model.Attribute
import io.rippledown.model.rule.CornerstoneStatus
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
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
        every { ruleService.attributeForName(any()) } returns null
        coEvery { reasonTransformer.transform(any()) } returns ReasonTransformation(message = "I do not understand")

        // When
        handler.handle(mapOf(REASON_PARAMETER to "allow"))

        // Then it is passed to the transformer as usual
        coVerify { reasonTransformer.transform("allow") }
    }

    @Test
    fun `should redirect a bare decline to the commit action when no cornerstone review is pending`() = runTest {
        // Given no cornerstone review is pending
        every { ruleService.cornerstoneStatus() } returns CornerstoneStatus(numberOfCornerstones = 0)

        // When the model routes a decline into the reason-transform function
        val result = handler.handle(mapOf(REASON_PARAMETER to "no"))

        // Then the model is redirected to commit the rule and no transform is attempted
        result shouldBe DECLINE_CORRECTION
        result shouldContain COMMIT_RULE
        coVerify(exactly = 0) { reasonTransformer.transform(any()) }
    }

    @Test
    fun `should redirect a bare decline to the cornerstone review when cornerstones remain`() = runTest {
        // Given two cornerstone cases remain to be reviewed
        val status = CornerstoneStatus(numberOfCornerstones = 2)
        every { ruleService.cornerstoneStatus() } returns status

        // When the model routes a decline into the reason-transform function
        val result = handler.handle(mapOf(REASON_PARAMETER to "No thanks."))

        // Then the model is redirected to the cornerstone review, and told the current status
        result shouldBe declineWithCornerstonesToReviewCorrection(status.summary())
        result shouldContain status.summary()
        coVerify(exactly = 0) { reasonTransformer.transform(any()) }
    }

    @Test
    fun `should treat text that merely starts with a decline as a reason`() = runTest {
        // Given no cornerstone review is pending
        every { ruleService.cornerstoneStatus() } returns CornerstoneStatus(numberOfCornerstones = 0)
        every { ruleService.attributeForName(any()) } returns null
        coEvery { reasonTransformer.transform(any()) } returns ReasonTransformation(message = "I do not understand")

        // When the user's text is more than a bare decline
        handler.handle(mapOf(REASON_PARAMETER to "no glucose in the case"))

        // Then it is transformed as usual
        coVerify { reasonTransformer.transform("no glucose in the case") }
    }

    @Test
    fun `should transform a genuine reason even while a cornerstone review is pending`() = runTest {
        // Given a cornerstone review is pending
        every { ruleService.cornerstoneStatus() } returns CornerstoneStatus(numberOfCornerstones = 2)
        every { ruleService.attributeForName(any()) } returns null
        coEvery { reasonTransformer.transform(any()) } returns ReasonTransformation(
            reasonId = 1,
            message = "wave height > 0.5"
        )

        // When the user provides a genuine reason
        handler.handle(mapOf(REASON_PARAMETER to "wave height is more than 0.5"))

        // Then it is transformed rather than redirected
        coVerify { reasonTransformer.transform("wave height is more than 0.5") }
    }

    @Test
    fun `should redirect an explanation request naming a known attribute to the explain action`() = runTest {
        // Given hasDiabetes is a known attribute
        val hasDiabetes = Attribute(1, "hasDiabetes")
        every { ruleService.attributeForName("hasDiabetes") } returns hasDiabetes

        // When the model routes the user's explanation request into the reason-transform function
        val result = handler.handle(mapOf(REASON_PARAMETER to "explain hasDiabetes"))

        // Then the model is redirected to emit the explanation action, and no transform is attempted
        result shouldBe explainRequestCorrection("hasDiabetes")
        result shouldContain EXPLAIN_AI_EVALUATION
        result shouldContain "\"attributeName\": \"hasDiabetes\""
        coVerify(exactly = 0) { reasonTransformer.transform(any()) }
    }

    @Test
    fun `should transform an explanation request that names no known attribute`() = runTest {
        // Given the named thing is not an attribute
        every { ruleService.attributeForName(any()) } returns null
        coEvery { reasonTransformer.transform(any()) } returns ReasonTransformation(message = "I do not understand")

        // When
        handler.handle(mapOf(REASON_PARAMETER to "explain the report"))

        // Then it is transformed as usual
        coVerify { reasonTransformer.transform("explain the report") }
    }

    @ParameterizedTest
    @ValueSource(strings = ["has \"type 2\" diabetes", "diabetes\\history"])
    fun `the correction should carry valid JSON when the attribute name needs escaping`(name: String) = runTest {
        // Given an attribute whose name needs JSON escaping
        every { ruleService.attributeForName(name) } returns Attribute(1, name)

        // When the model routes a request to explain it into the reason-transform function
        val result = handler.handle(mapOf(REASON_PARAMETER to "explain $name"))

        // Then the correction embeds a valid action object preserving the exact name
        val json = result.substringAfter("JSON object ").substringBefore(" and nothing else")
        json.fromJsonString<ActionComment>() shouldBe ActionComment(EXPLAIN_AI_EVALUATION, attributeName = name)
        coVerify(exactly = 0) { reasonTransformer.transform(any()) }
    }
}
