package io.rippledown.interpretation

import io.kotest.matchers.shouldBe
import io.rippledown.model.IntRangeData
import io.rippledown.model.RenderedComment
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Removal
import io.rippledown.model.diff.Replacement
import kotlin.test.Test

class CommentRowsTest {

    private val bondi = RenderedComment(
        text = "Go to Bondi.",
        conditions = listOf("Sun is in case"),
        name = "C1"
    )
    private val flippers = RenderedComment(
        text = "Bring your flippers.",
        conditions = listOf("Wave is in case"),
        name = "C2"
    )

    @Test
    fun `no change leaves every row unhighlighted`() {
        // Given comments and no rule session in progress
        // When the rows are computed
        val rows = commentRowsToDisplay(listOf(bondi, flippers))

        // Then nothing is highlighted and the comments are untouched
        rows shouldBe listOf(CommentRowState(bondi), CommentRowState(flippers))
    }

    @Test
    fun `no comments and no change gives no rows`() {
        commentRowsToDisplay(emptyList()) shouldBe emptyList()
    }

    @Test
    fun `an addition appends a row, where the comment will sit once committed`() {
        // When an addition is applied
        val rows = commentRowsToDisplay(
            listOf(bondi, flippers),
            Addition("Wear a hat.", "C3"),
            ruleConditions = listOf("Sex is F")
        )

        // Then the new row is last and marked as added
        rows.map { it.comment.name } shouldBe listOf("C1", "C2", "C3")
        rows.map { it.highlight } shouldBe listOf(
            CommentHighlight.NONE,
            CommentHighlight.NONE,
            CommentHighlight.ADDED
        )
    }

    @Test
    fun `an added row shows the comment being added, named, with the rule conditions`() {
        // Given a rule session with a condition so far
        // When an addition is applied
        val rows = commentRowsToDisplay(
            emptyList(),
            Addition("Wear a hat.", "C3"),
            ruleConditions = listOf("Sex is F")
        )

        // Then the row shows the pending comment, and its tooltip shows the
        // conditions of the rule being built, since it has no rule of its own yet
        rows.single().comment shouldBe RenderedComment(
            text = "Wear a hat.",
            conditions = listOf("Sex is F"),
            name = "C3"
        )
    }

    @Test
    fun `an addition is the only row when the case has no comments`() {
        val rows = commentRowsToDisplay(emptyList(), Addition("Wear a hat.", "C1"))

        rows.map { it.highlight } shouldBe listOf(CommentHighlight.ADDED)
    }

    /**
     * The rule has been committed, so the comment is on the case, but the client
     * has not yet been told that the session is over. Appending the pending row
     * as well would show the comment twice.
     */
    @Test
    fun `an addition already on the case is not shown twice`() {
        // Given a case whose comments include the one being added
        val rows = commentRowsToDisplay(listOf(bondi, flippers), Addition(flippers.text, flippers.name))

        // Then there is a row for each comment, and none is highlighted
        rows.map { it.comment.name } shouldBe listOf("C1", "C2")
        rows.map { it.highlight } shouldBe listOf(CommentHighlight.NONE, CommentHighlight.NONE)
    }

    @Test
    fun `an addition matched by name is not shown twice, whatever its rendered text`() {
        // Given a comment with a variable, rendered for this case
        val forThisCase = RenderedComment(text = "Glucose is 12.0.", name = "C1")

        // When the addition carries the text as rendered for another case
        val rows = commentRowsToDisplay(listOf(forThisCase), Addition("Glucose is 5.0.", "C1"))

        // Then the comment is shown once
        rows.map { it.comment.text } shouldBe listOf("Glucose is 12.0.")
    }

    @Test
    fun `a removal highlights the row of the comment being removed`() {
        // When a removal of the second comment is applied
        val rows = commentRowsToDisplay(listOf(bondi, flippers), Removal(flippers.text, flippers.name))

        // Then only that row is marked as removed, and no row is added
        rows.map { it.comment.text } shouldBe listOf(bondi.text, flippers.text)
        rows.map { it.highlight } shouldBe listOf(CommentHighlight.NONE, CommentHighlight.REMOVED)
    }

    @Test
    fun `a row being removed shows the conditions of the rule being built`() {
        // When a removal is applied during a session with a condition so far
        val rows = commentRowsToDisplay(
            listOf(bondi, flippers),
            Removal(bondi.text, bondi.name),
            ruleConditions = listOf("Sex is F")
        )

        // Then the row being removed carries them, in place of those of the rule
        // that gave it, since it is the removal the user is reviewing
        rows.first().comment.conditions shouldBe listOf("Sex is F")
        // And a row that is not being removed keeps its own
        rows.last().comment.conditions shouldBe flippers.conditions
    }

    /**
     * A comment with a variable renders differently from case to case, so the
     * text of the change, rendered against the case the rule is being built on,
     * need not match the text shown for a cornerstone. The attribute name
     * identifies the comment whatever the case.
     */
    @Test
    fun `a removal is matched by attribute name, not by rendered text`() {
        // Given a comment whose rendered text differs from the text of the change
        val forThisCase = RenderedComment(text = "Glucose is 12.0.", name = "C1")

        // When the removal carries the text as rendered for another case
        val rows = commentRowsToDisplay(listOf(forThisCase), Removal("Glucose is 5.0.", "C1"))

        // Then the row is still recognised as the one being removed
        rows.single().highlight shouldBe CommentHighlight.REMOVED
    }

    @Test
    fun `a removal without an attribute name falls back to matching the text`() {
        // Given a change made without a name, as an older client would send
        val rows = commentRowsToDisplay(listOf(bondi, flippers), Removal(flippers.text))

        // Then the row is matched by its text
        rows.map { it.highlight } shouldBe listOf(CommentHighlight.NONE, CommentHighlight.REMOVED)
    }

    @Test
    fun `a removal of a comment that is not shown highlights nothing`() {
        val rows = commentRowsToDisplay(listOf(bondi), Removal("Not on this case.", "C9"))

        rows.map { it.highlight } shouldBe listOf(CommentHighlight.NONE)
    }

    @Test
    fun `a replacement highlights the replaced row and carries the replacing comment`() {
        // When a replacement of the first comment is applied
        val rows = commentRowsToDisplay(
            listOf(bondi, flippers),
            Replacement(bondi.text, "Go to Maroubra.", "C3"),
            ruleConditions = listOf("Sex is F")
        )

        // Then that row is marked as replaced, and carries the replacing comment
        // with its own name and the conditions of the rule being built
        rows.map { it.highlight } shouldBe listOf(CommentHighlight.REPLACED, CommentHighlight.NONE)
        rows.first().comment shouldBe bondi
        rows.first().replacement shouldBe RenderedComment(
            text = "Go to Maroubra.",
            conditions = listOf("Sex is F"),
            name = "C3"
        )
    }

    /**
     * The name a replacement carries is that of the replacing attribute, which
     * is a different attribute from the one being replaced, so the row to
     * preview cannot be found by name.
     */
    @Test
    fun `a replacement is matched by the text of the comment being replaced`() {
        val rows = commentRowsToDisplay(
            listOf(bondi, flippers),
            Replacement(flippers.text, "Bring your board.", "C3")
        )

        rows.map { it.highlight } shouldBe listOf(CommentHighlight.NONE, CommentHighlight.REPLACED)
    }

    @Test
    fun `a row that is not being changed carries no replacement`() {
        val rows = commentRowsToDisplay(listOf(bondi, flippers), Replacement(bondi.text, "Go home.", "C3"))

        rows.last().replacement shouldBe null
    }

    @Test
    fun `the unresolved ranges of a comment are kept, so that the row can highlight them`() {
        // Given a comment with a variable that the case has no value for
        val unresolved = RenderedComment(
            text = "Glucose is {Glucose: no value}.",
            unresolvedRanges = listOf(IntRangeData(11, 29)),
            name = "C1"
        )

        // When the rows are computed
        val rows = commentRowsToDisplay(listOf(unresolved))

        // Then the ranges survive
        rows.single().comment.unresolvedRanges shouldBe listOf(IntRangeData(11, 29))
    }
}
