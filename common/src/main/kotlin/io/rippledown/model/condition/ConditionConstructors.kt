package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.condition.episodic.predicate.*
import io.rippledown.model.condition.episodic.signature.All
import io.rippledown.model.condition.episodic.signature.AtMost
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.episodic.signature.No
import io.rippledown.model.condition.series.Decreasing
import io.rippledown.model.condition.series.Increasing
import io.rippledown.model.condition.structural.IsAbsentFromCase
import io.rippledown.model.condition.structural.IsPresentInCase
import io.rippledown.model.condition.structural.IsSingleEpisodeCase

class ConditionConstructors {

    fun Low(attribute: Attribute, userExpression: String) =
        EpisodicCondition(null, attribute, Low, Current, userExpression)

    fun Normal(attribute: Attribute, userExpression: String) =
        EpisodicCondition(null, attribute, Normal, Current, userExpression)

    fun High(attribute: Attribute, userExpression: String) =
        EpisodicCondition(null, attribute, High, Current, userExpression)

    fun Present(attribute: Attribute, userExpression: String) =
        CaseStructureCondition(null, IsPresentInCase(attribute), userExpression)

    fun Absent(attribute: Attribute, userExpression: String) =
        CaseStructureCondition(null, IsAbsentFromCase(attribute), userExpression)

    fun Numeric(attribute: Attribute, userExpression: String) =
        EpisodicCondition(null, attribute, IsNumeric, Current, userExpression)

    fun NotNumeric(attribute: Attribute, userExpression: String) =
        EpisodicCondition(null, attribute, IsNotNumeric, Current, userExpression)

    fun Blank(attribute: Attribute, userExpression: String) =
        EpisodicCondition(null, attribute, IsBlank, Current, userExpression)

    fun NotBlank(attribute: Attribute, userExpression: String) =
        EpisodicCondition(null, attribute, IsNotBlank, Current, userExpression)

    fun Increasing(attribute: Attribute, userExpression: String) =
        SeriesCondition(null, attribute, Increasing, userExpression)

    fun Decreasing(attribute: Attribute, userExpression: String) =
        SeriesCondition(null, attribute, Decreasing, userExpression)

    fun Is(attribute: Attribute, userExpression: String, text: String) =
        EpisodicCondition(null, attribute, Is(text), Current, userExpression)

    fun Contains(attribute: Attribute, userExpression: String, text: String) =
        EpisodicCondition(null, attribute, Contains(text), Current, userExpression)

    fun DoesNotContain(attribute: Attribute, userExpression: String, text: String) =
        EpisodicCondition(null, attribute, DoesNotContain(text), Current, userExpression)

    fun GreaterThanOrEqualTo(attribute: Attribute, userExpression: String, d: String) =
        EpisodicCondition(null, attribute, GreaterThanOrEquals(d.toDoubleOrNull()!!), Current, userExpression)

    fun LessThanOrEqualTo(attribute: Attribute, userExpression: String, d: String) =
        EpisodicCondition(null, attribute, LessThanOrEquals(d.toDoubleOrNull()!!), Current, userExpression)

    fun SlightlyLow(attribute: Attribute, userExpression: String, cutoff: String) =
        EpisodicCondition(null, attribute, LowByAtMostSomePercentage(cutoff.toInt()), Current, userExpression)

    fun NormalOrSlightlyLow(attribute: Attribute, userExpression: String, cutoff: String) =
        EpisodicCondition(null, attribute, NormalOrLowByAtMostSomePercentage(cutoff.toInt()), Current, userExpression)

    fun SlightlyHigh(attribute: Attribute, userExpression: String, cutoff: String) =
        EpisodicCondition(null, attribute, HighByAtMostSomePercentage(cutoff.toInt()), Current, userExpression)

    fun NormalOrSlightlyHigh(
        attribute: Attribute,
        userExpression: String,
        cutoff: String
    ) = EpisodicCondition(null, attribute, NormalOrHighByAtMostSomePercentage(cutoff.toInt()), Current, userExpression)

    fun SingleEpisodeCase(userExpression: String) = CaseStructureCondition(null, IsSingleEpisodeCase, userExpression)

    fun AllNormal(attribute: Attribute, userExpression: String) =
        EpisodicCondition(null, attribute, Normal, All, userExpression)

    fun NoNormal(attribute: Attribute, userExpression: String) =
        EpisodicCondition(null, attribute, Normal, No, userExpression)

    fun AllHigh(attribute: Attribute, userExpression: String) =
        EpisodicCondition(null, attribute, High, All, userExpression)

    fun NoHigh(attribute: Attribute, userExpression: String) =
        EpisodicCondition(null, attribute, High, No, userExpression)

    fun AllLow(attribute: Attribute, userExpression: String) =
        EpisodicCondition(null, attribute, Low, All, userExpression)

    fun NoLow(attribute: Attribute, userExpression: String) =
        EpisodicCondition(null, attribute, Low, No, userExpression)

    fun AllContain(attribute: Attribute, userExpression: String, text: String) =
        EpisodicCondition(null, attribute, Contains(text), All, userExpression)

    fun NoContain(attribute: Attribute, userExpression: String, text: String) =
        EpisodicCondition(null, attribute, Contains(text), No, userExpression)

    fun AllNumeric(attribute: Attribute, userExpression: String) =
        EpisodicCondition(null, attribute, IsNumeric, All, userExpression)

    fun NoNumeric(attribute: Attribute, userExpression: String) =
        EpisodicCondition(null, attribute, IsNumeric, No, userExpression)

    fun AtMostHigh(attribute: Attribute, userExpression: String, count: String) =
        EpisodicCondition(null, attribute, High, AtMost(count.toInt()), userExpression)

    fun AtMostLow(attribute: Attribute, userExpression: String, count: String) =
        EpisodicCondition(null, attribute, Low, AtMost(count.toInt()), userExpression)

    fun AtMostGreaterThanOrEqualTo(attribute: Attribute, userExpression: String, count: String, cutoff: String) =
        EpisodicCondition(
            null,
            attribute,
            GreaterThanOrEquals(cutoff.toDoubleOrNull()!!),
            AtMost(count.toInt()),
            userExpression
        )
}