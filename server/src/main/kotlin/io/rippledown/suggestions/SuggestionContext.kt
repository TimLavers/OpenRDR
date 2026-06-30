package io.rippledown.suggestions

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.rule.RuleTree
import io.rippledown.model.rule.RuleTreeChange

/**
 * Hard cap on the number of suggestions returned by [ConditionSuggester].
 *
 * Rationale (see `documentation/design/targeted_suggested_conditions.md`,
 * "Suggestion cap"):
 *  - The chat UI shows ~5 suggestions by default, expandable to ~10. Twenty is
 *    roughly four pages of expansion — the realistic scrollable maximum.
 *  - The LLM-facing list (in `SuggestedConditionsHandler`) is much easier to
 *    disambiguate at 20 entries than at fifty.
 *
 * The cap applies *after* ranking, so it removes the lowest-relevance entries —
 * never a top-ranked candidate. Phases 2/3 may tighten this once LLM/embedding
 * signals consistently push the right answer into the top 10.
 */
const val MAX_SUGGESTIONS: Int = 20

/**
 * Inputs threaded into [ConditionSuggester] so that scorers can reason about
 * the rule action, the cornerstone cases of the active session and the existing
 * rule tree — in addition to the session case and attribute set the suggester
 * already needed.
 *
 * Defaults are deliberately permissive: `action = null`, `cornerstones = empty`
 * and an empty `ruleTree` model an "outside-an-active-session" call. With those
 * defaults every action-aware scorer collapses to zero, so the ranker degrades
 * cleanly to the alphabetic tiebreak — i.e. today's behaviour.
 */
data class SuggestionContext(
    val sessionCase: RDRCase,
    val attributes: Set<Attribute>,
    val action: RuleTreeChange? = null,
    val cornerstones: List<RDRCase> = emptyList(),
    val ruleTree: RuleTree = RuleTree()
)
