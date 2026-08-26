package io.rippledown.interpretation

import io.rippledown.model.RenderedComment
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Diff
import io.rippledown.model.diff.Removal
import io.rippledown.model.diff.Replacement

/**
 * How a comment row should be highlighted, given the change the rule session
 * in progress is about to make.
 */
enum class CommentHighlight { NONE, ADDED, REMOVED, REPLACED }

/**
 * A comment row to draw, together with the pending change, if any, that
 * affects it.
 *
 * @param replacement for [CommentHighlight.REPLACED], the comment that the
 *   rule being built will give in place of [comment]. It is shown beside the
 *   comment being replaced, each with its own name, so that the user sees what
 *   is going and what is coming in its place.
 */
data class CommentRowState(
    val comment: RenderedComment,
    val highlight: CommentHighlight = CommentHighlight.NONE,
    val replacement: RenderedComment? = null
)

/**
 * The rows the Comments panel should draw for [comments], given the [diff] the
 * rule session in progress is about to make.
 *
 * A pending addition is not on the case yet, so a row for it is appended: a new
 * comment attribute has the highest id, and comments are shown in attribute id
 * order, so this is where the comment will sit once the rule is committed. A
 * pending removal or replacement applies to a row that is already there, and
 * that row shows the comment as the change gives it: a comment with variables is
 * previewed as the template its rule defines, not as it renders for this case.
 *
 * @param ruleConditions the conditions of the rule being built, shown in the
 *   tooltip of a comment that is being added or is replacing another, since
 *   neither has a rule of its own yet.
 */
fun commentRowsToDisplay(
    comments: List<RenderedComment>,
    diff: Diff? = null,
    ruleConditions: List<String> = emptyList()
): List<CommentRowState> {
    if (diff == null) return comments.map { CommentRowState(it) }

    return when (diff) {
        // The row shows the comment as the change gives it, and the conditions
        // of the rule being built rather than those of the rule that gave the
        // comment, because it is the removal that the user is reviewing.
        is Removal -> comments.map {
            if (it.isThatOf(diff.removedText, diff.attributeName)) {
                CommentRowState(
                    it.copy(text = diff.removedText, conditions = ruleConditions),
                    CommentHighlight.REMOVED
                )
            } else {
                CommentRowState(it)
            }
        }

        // The row being replaced is found by the name of the attribute going,
        // since diff.attributeName is that of the one coming in its place, and
        // shows the comment as the change gives it.
        is Replacement -> comments.map {
            if (it.isThatOf(diff.originalText, diff.replacedAttributeName)) {
                CommentRowState(
                    comment = it.copy(text = diff.originalText),
                    highlight = CommentHighlight.REPLACED,
                    replacement = RenderedComment(
                        text = diff.replacementText,
                        conditions = ruleConditions,
                        name = diff.attributeName
                    )
                )
            } else {
                CommentRowState(it)
            }
        }

        // The comment can already be on the case: the rule has been committed
        // but the client has not yet been told the session is over. Showing the
        // pending row as well would show the comment twice.
        is Addition -> comments.map { CommentRowState(it) } +
                if (comments.any { it.isThatOf(diff.addedText, diff.attributeName) }) {
                    emptyList()
                } else {
                    listOf(
                        CommentRowState(
                            comment = RenderedComment(
                                text = diff.addedText,
                                conditions = ruleConditions,
                                name = diff.attributeName
                            ),
                            highlight = CommentHighlight.ADDED
                        )
                    )
                }
    }
}

/**
 * Whether this is the comment the given change concerns. The attribute name
 * identifies it where the change carries one, which is the reliable test: a
 * comment with variables renders differently from case to case, so the text of
 * the change, rendered against the case the rule is being built on, need not
 * match the text shown for a cornerstone. Changes made without a name, as the
 * UI's own tests and older clients do, fall back to matching the text.
 */
private fun RenderedComment.isThatOf(changedText: String, changedAttributeName: String) =
    if (changedAttributeName.isNotEmpty() && name.isNotEmpty()) {
        name == changedAttributeName
    } else {
        text == changedText
    }
