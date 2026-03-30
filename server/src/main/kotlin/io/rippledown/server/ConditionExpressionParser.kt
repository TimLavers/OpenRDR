package io.rippledown.server

import io.rippledown.model.Attribute
import io.rippledown.model.condition.CaseStructureCondition
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.SeriesCondition
import io.rippledown.model.condition.episodic.predicate.*
import io.rippledown.model.condition.episodic.signature.AtLeast
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.series.Decreasing
import io.rippledown.model.condition.series.Increasing
import io.rippledown.model.condition.structural.IsAbsentFromCase
import io.rippledown.model.condition.structural.IsPresentInCase
import io.rippledown.model.condition.structural.IsSingleEpisodeCase

/**
 * Deterministic parser that converts human-readable condition expressions
 * into Condition objects without using AI/Gemini.
 *
 * Supported patterns:
 *  - case is for a single date
 *  - at least N ATTR are numeric
 *  - ATTR increasing
 *  - ATTR decreasing
 *  - ATTR is in case
 *  - ATTR is not in case
 *  - ATTR does not contain "TEXT"
 *  - ATTR contains "TEXT"
 *  - ATTR is normal or high by at most N%
 *  - ATTR is normal or low by at most N%
 *  - ATTR is low by at most N%
 *  - ATTR is high by at most N%
 *  - ATTR is normal
 *  - ATTR is high
 *  - ATTR is low
 *  - ATTR is "VALUE"
 *  - ATTR ≥ VALUE
 *  - ATTR ≤ VALUE
 */
class ConditionExpressionParser(private val attributeFor: (String) -> Attribute) {

    fun parse(expression: String): Condition {
        val text = expression.trim()

        // case is for a single date
        if (text == "case is for a single date") {
            return CaseStructureCondition(null, IsSingleEpisodeCase, text)
        }

        // at least N ATTR are numeric
        AT_LEAST_NUMERIC.matchEntire(text)?.let { match ->
            val n = match.groupValues[1].toInt()
            val attr = attributeFor(match.groupValues[2])
            return EpisodicCondition(null, attr, IsNumeric, AtLeast(n), text)
        }

        // ATTR increasing
        if (text.endsWith(" increasing")) {
            val attrName = text.removeSuffix(" increasing")
            return SeriesCondition(null, attributeFor(attrName), Increasing, text)
        }

        // ATTR decreasing
        if (text.endsWith(" decreasing")) {
            val attrName = text.removeSuffix(" decreasing")
            return SeriesCondition(null, attributeFor(attrName), Decreasing, text)
        }

        // ATTR is not in case
        if (text.endsWith(" is not in case")) {
            val attrName = text.removeSuffix(" is not in case")
            return CaseStructureCondition(null, IsAbsentFromCase(attributeFor(attrName)), text)
        }

        // ATTR is in case
        if (text.endsWith(" is in case")) {
            val attrName = text.removeSuffix(" is in case")
            return CaseStructureCondition(null, IsPresentInCase(attributeFor(attrName)), text)
        }

        // ATTR does not contain "TEXT"
        DOES_NOT_CONTAIN.matchEntire(text)?.let { match ->
            val attr = attributeFor(match.groupValues[1])
            val value = match.groupValues[2]
            return EpisodicCondition(null, attr, DoesNotContain(value), Current, text)
        }

        // ATTR contains "TEXT"
        CONTAINS.matchEntire(text)?.let { match ->
            val attr = attributeFor(match.groupValues[1])
            val value = match.groupValues[2]
            return EpisodicCondition(null, attr, Contains(value), Current, text)
        }

        // ATTR is normal or high by at most N%
        NORMAL_OR_HIGH_BY_AT_MOST.matchEntire(text)?.let { match ->
            val attr = attributeFor(match.groupValues[1])
            val pct = match.groupValues[2].toInt()
            return EpisodicCondition(null, attr, NormalOrHighByAtMostSomePercentage(pct), Current, text)
        }

        // ATTR is normal or low by at most N%
        NORMAL_OR_LOW_BY_AT_MOST.matchEntire(text)?.let { match ->
            val attr = attributeFor(match.groupValues[1])
            val pct = match.groupValues[2].toInt()
            return EpisodicCondition(null, attr, NormalOrLowByAtMostSomePercentage(pct), Current, text)
        }

        // ATTR is low by at most N%
        LOW_BY_AT_MOST.matchEntire(text)?.let { match ->
            val attr = attributeFor(match.groupValues[1])
            val pct = match.groupValues[2].toInt()
            return EpisodicCondition(null, attr, LowByAtMostSomePercentage(pct), Current, text)
        }

        // ATTR is high by at most N%
        HIGH_BY_AT_MOST.matchEntire(text)?.let { match ->
            val attr = attributeFor(match.groupValues[1])
            val pct = match.groupValues[2].toInt()
            return EpisodicCondition(null, attr, HighByAtMostSomePercentage(pct), Current, text)
        }

        // ATTR is normal
        if (text.endsWith(" is normal")) {
            val attrName = text.removeSuffix(" is normal")
            return EpisodicCondition(null, attributeFor(attrName), Normal, Current, text)
        }

        // ATTR is high
        if (text.endsWith(" is high")) {
            val attrName = text.removeSuffix(" is high")
            return EpisodicCondition(null, attributeFor(attrName), High, Current, text)
        }

        // ATTR is low
        if (text.endsWith(" is low")) {
            val attrName = text.removeSuffix(" is low")
            return EpisodicCondition(null, attributeFor(attrName), Low, Current, text)
        }

        // ATTR is "VALUE"
        IS_QUOTED.matchEntire(text)?.let { match ->
            val attr = attributeFor(match.groupValues[1])
            val value = match.groupValues[2]
            return EpisodicCondition(null, attr, Is(value), Current, text)
        }

        // ATTR ≥ VALUE
        GTE.matchEntire(text)?.let { match ->
            val attr = attributeFor(match.groupValues[1])
            val value = match.groupValues[2].toDouble()
            return EpisodicCondition(null, attr, GreaterThanOrEquals(value), Current, text)
        }

        // ATTR ≤ VALUE
        LTE.matchEntire(text)?.let { match ->
            val attr = attributeFor(match.groupValues[1])
            val value = match.groupValues[2].toDouble()
            return EpisodicCondition(null, attr, LessThanOrEquals(value), Current, text)
        }

        throw IllegalArgumentException("Unrecognised condition expression: '$text'")
    }

    companion object {
        private val AT_LEAST_NUMERIC = Regex("""at least (\d+) (.+) are numeric""")
        private val DOES_NOT_CONTAIN = Regex("""(.+) does not contain "(.+)"""")
        private val CONTAINS = Regex("""(.+) contains "(.+)"""")
        private val NORMAL_OR_HIGH_BY_AT_MOST = Regex("""(.+) is normal or high by at most (\d+)%""")
        private val NORMAL_OR_LOW_BY_AT_MOST = Regex("""(.+) is normal or low by at most (\d+)%""")
        private val LOW_BY_AT_MOST = Regex("""(.+) is low by at most (\d+)%""")
        private val HIGH_BY_AT_MOST = Regex("""(.+) is high by at most (\d+)%""")
        private val IS_QUOTED = Regex("""(.+) is "(.+)"""")
        private val GTE = Regex("""(.+) ≥ (.+)""")
        private val LTE = Regex("""(.+) ≤ (.+)""")
    }
}
