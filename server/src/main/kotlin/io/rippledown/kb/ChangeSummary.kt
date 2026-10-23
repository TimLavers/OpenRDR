package io.rippledown.kb

import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.condition.ConditionText
import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.Rule

/**
 * One-line summaries of the changes recorded in a knowledge base's change
 * log, written for a colleague reading the history rather than for the user
 * who made the change. See documentation/design/concurrent_users.md.
 */

/**
 * The summary of the rules a session added, from one of them: the rules of
 * a session share an action and conditions. [describe] gives the text of an
 * assignment, and is supplied by the knowledge base, which can resolve a
 * by-definition assignment to its definition.
 */
fun rulesAddedSummary(exemplar: Rule, describe: (AssignValue) -> String): String {
    val parentAssignment = exemplar.parent?.assignment
    val assignment = exemplar.assignment
    val action = when {
        parentAssignment == null -> {
            val added = checkNotNull(assignment) { "A rule under the root assigns something." }
            "${if (added.isComment()) "Added comment" else "Assigned"} ${added.text(describe)}"
        }

        assignment == null ->
            "${if (parentAssignment.isComment()) "Removed comment" else "Retracted"} ${parentAssignment.text(describe)}"

        else ->
            "${if (assignment.isComment()) "Replaced comment" else "Replaced"} ${parentAssignment.text(describe)} " +
                    "with ${assignment.text(describe)}"
    }
    return "$action ${conditionsText(exemplar)}."
}

fun rulesUndoneSummary(summaryOfRulesAdded: String) = "Undid: $summaryOfRulesAdded"

fun attributeMovedSummary(moved: Attribute, destination: Attribute) =
    "Moved \"${moved.name}\" to the position of \"${destination.name}\" in the case view."

fun caseDeletedSummary(caseName: String, listName: String? = null) =
    if (listName == null) "Deleted case \"$caseName\"." else "Deleted case \"$caseName\" from the \"$listName\" list."

fun descriptionSetSummary() = "Changed the description."

private fun AssignValue.isComment() = attribute.kind == AttributeKind.COMMENT

private fun AssignValue.text(describe: (AssignValue) -> String) =
    if (isComment()) "\"${describe(this)}\"" else describe(this)

private fun conditionsText(rule: Rule): String {
    if (rule.conditions.isEmpty()) return "with no conditions"
    val texts = rule.conditions
        .map { ConditionText.of(it) }
        .map { if (it.hasPhrase()) it.phrase else it.formal }
        .sortedWith(String.CASE_INSENSITIVE_ORDER)
    return "when ${texts.joinToString(", ")}"
}
