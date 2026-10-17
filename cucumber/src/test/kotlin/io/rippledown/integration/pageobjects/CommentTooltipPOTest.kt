package io.rippledown.integration.pageobjects

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.interpretation.CommentRowState
import io.rippledown.interpretation.CommentTooltipTestHook
import io.rippledown.model.RenderedComment
import io.rippledown.model.condition.ConditionText
import org.junit.jupiter.api.Test

class CommentTooltipPOTest {
    private val hook = CommentTooltipTestHook()
    private val page = CommentTooltipPO(hook)

    @Test
    fun `condition lookup selects the exact comment without mixing rows`() {
        // Given
        val expected = listOf(ConditionText("Sun is in case", "sunshine"))
        hook.update(
            listOf(
                CommentRowState(RenderedComment("Beach", conditions = expected)),
                CommentRowState(RenderedComment("Beach time", conditions = listOf(ConditionText("Wave is in case"))))
            )
        )

        // When
        val conditions = page.conditionsFor("Beach")

        // Then
        conditions shouldBe expected
    }

    @Test
    fun `missing comments cannot pass an empty conditions check`() {
        // Given an empty snapshot
        // When
        val error = shouldThrow<AssertionError> { page.conditionsFor("Missing") }

        // Then
        error.message.orEmpty().contains("Missing") shouldBe true
    }

    @Test
    fun `ambiguous comment text fails rather than selecting an arbitrary tooltip`() {
        // Given
        hook.update(
            listOf(
                CommentRowState(RenderedComment("Beach", name = "C1")),
                CommentRowState(RenderedComment("Beach", name = "C2"))
            )
        )

        // When
        val error = shouldThrow<AssertionError> { page.conditionsFor("Beach") }

        // Then
        error.message.orEmpty().contains("C1") shouldBe true
        error.message.orEmpty().contains("C2") shouldBe true
    }

    @Test
    fun `phrases match tooltip rendering excluding blank and identical formal text`() {
        // Given
        hook.update(
            listOf(
                CommentRowState(
                    RenderedComment(
                        "Beach", conditions = listOf(
                            ConditionText("A"), ConditionText("B", "  "), ConditionText("C", "C"),
                            ConditionText("D", "waves"), ConditionText("E", "waves")
                        )
                    )
                )
            )
        )

        // When
        val phrases = page.phrasesFor("Beach")

        // Then
        phrases shouldBe listOf("waves", "waves")
    }

    @Test
    fun `checks succeed without any accessibility context or pointer interaction`() {
        // Given
        hook.update(
            listOf(
                CommentRowState(RenderedComment("Beach", conditions = listOf(ConditionText("Sun", "sunshine")))),
                CommentRowState(RenderedComment("Home"))
            )
        )

        // When
        page.waitForConditions("Beach", listOf("Sun"))
        page.waitForPhrases("Beach", listOf("sunshine"))
        page.waitForConditions("Home", emptyList())

        // Then
        page.conditionsFor("Home") shouldBe emptyList()
    }

    @Test
    fun `condition check waits for the right row and for extra old conditions to disappear`() {
        // Given snapshots from consecutive compositions
        val hook = mockk<CommentTooltipTestHook>()
        val expected = CommentTooltipTestHook.Tooltip("Beach", "C1", listOf(ConditionText("Sun")))
        every { hook.snapshot() } returnsMany listOf(
            emptyList(),
            listOf(expected.copy(text = "Other comment")),
            listOf(expected.copy(conditions = expected.conditions + ConditionText("Wave"))),
            listOf(expected)
        )

        // When
        CommentTooltipPO(hook).waitForConditions("Beach", listOf("Sun"))

        // Then all four snapshots were required, not merely the first containing Sun
        verify(exactly = 4) { hook.snapshot() }
    }

    @Test
    fun `phrase check observes a rename even when the formal condition is unchanged`() {
        // Given
        val hook = mockk<CommentTooltipTestHook>()
        every { hook.snapshot() } returnsMany listOf(
            listOf(CommentTooltipTestHook.Tooltip("Beach", "C1", listOf(ConditionText("Sun", "hot")))),
            listOf(CommentTooltipTestHook.Tooltip("Beach", "C1", listOf(ConditionText("Sun", "sunshine"))))
        )

        // When
        CommentTooltipPO(hook).waitForPhrases("Beach", listOf("sunshine"))

        // Then
        verify(exactly = 2) { hook.snapshot() }
    }
}
