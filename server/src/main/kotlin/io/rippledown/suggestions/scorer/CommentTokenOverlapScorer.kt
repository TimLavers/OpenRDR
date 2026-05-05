package io.rippledown.suggestions.scorer

import io.rippledown.model.Attribute
import io.rippledown.model.condition.CaseStructureCondition
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.SeriesCondition
import io.rippledown.model.condition.edit.SuggestedCondition
import io.rippledown.model.condition.episodic.predicate.*
import io.rippledown.model.condition.series.Decreasing
import io.rippledown.model.condition.series.Increasing
import io.rippledown.model.condition.series.SeriesPredicate
import io.rippledown.model.condition.structural.CaseStructurePredicate
import io.rippledown.model.condition.structural.IsAbsentFromCase
import io.rippledown.model.condition.structural.IsPresentInCase
import io.rippledown.model.condition.structural.IsSingleEpisodeCase
import io.rippledown.model.rule.ChangeTreeToAddConclusion
import io.rippledown.model.rule.ChangeTreeToRemoveConclusion
import io.rippledown.model.rule.ChangeTreeToReplaceConclusion
import io.rippledown.suggestions.SuggestionContext

/**
 * Scores a [SuggestedCondition] by the number of tokens it shares with the
 * comment text of the current rule action.
 *
 * The intent is: when a user types "TSH is high", a candidate
 * `EpisodicCondition(TSH, High, …)` should beat `EpisodicCondition(TSH, Low, …)`
 * even when there is no historical signal — the user just told us which
 * direction matters.
 *
 * Token sources:
 *  - **Comment**: lowercase, split on non-alphanumerics, drop a small set of
 *    function words.
 *  - **Condition**: the candidate's attribute name plus a small,
 *    hand-curated direction-word vocabulary keyed off the predicate type.
 *    Signature tokens (`Current`, `All`, `AtLeast(n)`, `AtMost(n)`, `No`) are
 *    deliberately excluded — they pollute scores ("at least 1" matches every
 *    comment containing "at") and users rarely phrase comments that way.
 *
 * Score is `|commentTokens ∩ conditionTokens|`. Null action → 0 for every
 * candidate, and the ranker degrades to other signals.
 */
internal class CommentTokenOverlapScorer(
    private val ctx: SuggestionContext,
) : SuggestionScorer {

    private val commentTokens: Set<String> = computeCommentTokens()

    override fun score(s: SuggestedCondition): Int {
        if (commentTokens.isEmpty()) return 0
        val candidateTokens = tokensFor(s.initialSuggestion())
        return commentTokens.intersect(candidateTokens).size
    }

    private fun computeCommentTokens(): Set<String> {
        val text = when (val action = ctx.action) {
            is ChangeTreeToAddConclusion -> action.toBeAdded.text
            is ChangeTreeToReplaceConclusion -> action.replacement.text
            is ChangeTreeToRemoveConclusion -> action.toBeRemoved.text
            else -> return emptySet()
        }
        return tokenise(text)
    }

    companion object {
        /**
         * Words that carry no signal in clinical-style comments. Kept small
         * and conservative; expanding it (synonyms, stems, multi-word
         * phrases) is Phase 3 work.
         */
        private val STOPWORDS = setOf(
            "is", "the", "a", "an", "of", "to", "in", "for", "with",
            "and", "or", "not", "are", "was", "were", "be", "been",
        )

        internal fun tokenise(text: String): Set<String> = text
            .lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.isNotEmpty() && it !in STOPWORDS }
            .toSet()

        /**
         * Maximum number of tokens an `Is`/`Contains` value may contribute.
         * Short, code-like values (`"M"`, `"stable"`, `"low risk"`) are the
         * legitimate use case. Long free-text values — typically stored on
         * comment-style attributes such as `PSA Comment` — would otherwise
         * accumulate dozens of generic tokens (`further`, `review`, `patient`,
         * …) and win on accidental collisions with the action comment, which
         * is exactly the bug this cap prevents. Provenance-based surfacing of
         * previously-used long `Contains` conditions is still handled by
         * [HistoricalRuleScorer], which runs ahead of this scorer.
         */
        private const val MAX_VALUE_TOKENS = 3

        internal fun valueTokens(value: String): Set<String> {
            val tokens = tokenise(value)
            return if (tokens.size > MAX_VALUE_TOKENS) emptySet() else tokens
        }

        internal fun tokensFor(condition: Condition): Set<String> = when (condition) {
            is EpisodicCondition -> tokensFor(condition.attribute) + tokensFor(condition.predicate)
            is SeriesCondition -> tokensFor(condition.attribute) + tokensFor(condition.seriesPredicate)
            is CaseStructureCondition -> tokensFor(condition.predicate)
            else -> emptySet()
        }

        private fun tokensFor(attribute: Attribute): Set<String> = tokenise(attribute.name)

        private fun tokensFor(p: TestResultPredicate): Set<String> = when (p) {
            High -> setOf("high")
            Low -> setOf("low")
            Normal -> setOf("normal")
            is HighByAtMostSomePercentage -> setOf("high")
            is NormalOrHighByAtMostSomePercentage -> setOf("high", "normal")
            is LowByAtMostSomePercentage -> setOf("low")
            is NormalOrLowByAtMostSomePercentage -> setOf("low", "normal")
            is GreaterThanOrEquals, is GreaterThan -> setOf("high", "above", "greater")
            is LessThanOrEquals, is LessThan -> setOf("low", "below", "less")
            IsNumeric, IsNotNumeric -> setOf("numeric")
            is Is -> valueTokens(p.toFind)
            is IsNot -> valueTokens(p.toFind)
            is Contains -> valueTokens(p.toFind)
            is DoesNotContain -> valueTokens(p.toFind)
            else -> emptySet()
        }

        private fun tokensFor(p: SeriesPredicate): Set<String> = when (p) {
            is Increasing -> setOf("increasing", "rising", "trend")
            is Decreasing -> setOf("decreasing", "falling", "trend")
        }

        private fun tokensFor(p: CaseStructurePredicate): Set<String> = when (p) {
            is IsPresentInCase -> tokensFor(p.attribute) + setOf("present")
            is IsAbsentFromCase -> tokensFor(p.attribute) + setOf("absent", "missing")
            IsSingleEpisodeCase -> setOf("single", "episode")
        }
    }
}
