package io.rippledown.kb.chat.action

import io.rippledown.model.rule.CornerstoneStatus

/**
 * Builds the message that an action which advances cornerstone review (exempt /
 * next / previous) sends back to the model after the rule engine has recomputed
 * the cornerstones.
 *
 * In addition to the bare cornerstone status, this prepends a directive in
 * the no-remaining-cornerstones case (`Total == 0`). Without it, the model is
 * prone to falling back into "Here are some suggestions" once Total reaches
 * 0, instead of committing the rule, even though the system prompt's
 * "Allowing or Disallowing the change to the Cornerstone Case report" Step 5
 * tells it to commit. Making the instruction explicit in the same turn that
 * Total reaches 0 makes the contract harder to ignore.
 */
internal fun CornerstoneStatus.endOfReviewMessage(): String {
    val base = summary()
    if (numberOfCornerstones != 0) return base
    return base + "\n" +
            "All cornerstone cases have been reviewed. If the user has already " +
            "declined to provide further reasons, you MUST respond with " +
            "{\"action\": \"CommitRule\"} now and nothing else. Do NOT ask for more " +
            "reasons and do NOT call getSuggestedConditions."
}

/**
 * Builds the message sent back to the model after a reason has been removed
 * from the rule being built.
 *
 * Removing a reason does not advance cornerstone review, so this must not use
 * [endOfReviewMessage]: with no cornerstones left to review, its commit
 * directive led the model to commit the rule in the very turn the user removed
 * a reason, instead of confirming the removal. A user who is taking reasons
 * back is still editing the rule, whatever they said about further reasons
 * earlier.
 */
internal fun CornerstoneStatus.reasonRemovedMessage(): String =
    summary() + "\n" +
            "The reason has been removed from the rule being built. Tell the user " +
            "it has been removed and ask whether they want to give any more " +
            "reasons. Do NOT commit the rule in this turn."
