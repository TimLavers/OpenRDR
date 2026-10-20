package io.rippledown.interpretation

import io.kotest.matchers.shouldBe
import io.rippledown.model.RenderedComment
import io.rippledown.model.condition.ConditionText
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Removal
import io.rippledown.model.diff.Replacement
import org.junit.jupiter.api.Test

class CommentTooltipTestHookTest {
    private val original = RenderedComment(
        "Go to Bondi.", name = "C1",
        conditions = listOf(ConditionText("Sun is in case", "sunshine"))
    )
    private val pending = listOf(ConditionText("Wave is in case", "waves"))

    @Test
    fun `snapshots contain each comment's own tooltip conditions`() {
        // Given
        val hook = CommentTooltipTestHook()
        val other = RenderedComment("Stay home.", name = "C2")

        // When
        hook.update(commentRowsToDisplay(listOf(original, other)))

        // Then
        hook.snapshot().map { it.text } shouldBe listOf("Go to Bondi.", "Stay home.")
        hook.snapshot().map { it.conditions } shouldBe listOf(original.conditions, emptyList())
    }

    @Test
    fun `pending additions and removals expose the conditions of the rule in progress`() {
        // Given
        val hook = CommentTooltipTestHook()

        // When
        hook.update(commentRowsToDisplay(emptyList(), Addition(original.text, "C1"), pending))
        val addition = hook.snapshot()
        hook.update(commentRowsToDisplay(listOf(original), Removal(original.text, "C1"), pending))

        // Then
        addition.single().conditions shouldBe pending
        hook.snapshot().single().conditions shouldBe pending
    }

    @Test
    fun `replacement exposes both the old and new comment tooltips`() {
        // Given
        val hook = CommentTooltipTestHook()

        // When
        hook.update(commentRowsToDisplay(listOf(original), Replacement(original.text, "Go to Manly."), pending))

        // Then
        hook.snapshot().map { it.text } shouldBe listOf(original.text, "Go to Manly.")
        hook.snapshot().map { it.conditions } shouldBe listOf(original.conditions, pending)
    }

    @Test
    fun `updates replace previous rows and clearing one window leaves another unchanged`() {
        // Given
        val firstWindow = CommentTooltipTestHook()
        val secondWindow = CommentTooltipTestHook()
        firstWindow.snapshot() shouldBe emptyList()
        firstWindow.update(commentRowsToDisplay(listOf(original)))
        secondWindow.update(commentRowsToDisplay(listOf(original)))

        // When
        firstWindow.update(commentRowsToDisplay(listOf(RenderedComment("New case"))))
        val updated = firstWindow.snapshot()
        firstWindow.clear()

        // Then
        updated.map { it.text } shouldBe listOf("New case")
        firstWindow.snapshot() shouldBe emptyList()
        secondWindow.snapshot().single().text shouldBe original.text
    }

    @Test
    fun `published conditions are copied so later input mutations cannot change a snapshot`() {
        // Given
        val hook = CommentTooltipTestHook()
        val conditions = original.conditions.toMutableList()
        hook.update(commentRowsToDisplay(listOf(original.copy(conditions = conditions))))
        val snapshot = hook.snapshot()

        // When
        conditions.clear()
        hook.update(emptyList())

        // Then
        snapshot.single().conditions shouldBe original.conditions
        hook.snapshot() shouldBe emptyList()
    }
}
