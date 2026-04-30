package io.rippledown.kb.chat.action

import io.rippledown.model.rule.CornerstoneStatus

/**
 * Builds the message that an action which advances cornerstone review (exempt /
 * next / previous / remove-reason) sends back to the model after the rule
 * engine has recomputed the cornerstones.
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
