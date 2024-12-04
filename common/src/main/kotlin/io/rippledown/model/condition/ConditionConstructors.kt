package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.condition.episodic.predicate.*
import io.rippledown.model.condition.episodic.signature.*
import io.rippledown.model.condition.series.Decreasing
import io.rippledown.model.condition.series.Increasing
import io.rippledown.model.condition.structural.IsAbsentFromCase
import io.rippledown.model.condition.structural.IsPresentInCase
import io.rippledown.model.condition.structural.IsSingleEpisodeCase

class ConditionConstructors {

    //Zero-parameter conditions
    fun SingleEpisodeCase(userExpression: String) = CaseStructureCondition(null, IsSingleEpisodeCase, userExpression)

    //One-parameter conditions
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

    //Two-parameter conditions
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

    fun AllDoNotContainText(attribute: Attribute, userExpression: String, text: String) =
        EpisodicCondition(null, attribute, DoesNotContain(text), All, userExpression)

    fun AllNumeric(attribute: Attribute, userExpression: String) =
        EpisodicCondition(null, attribute, IsNumeric, All, userExpression)

    fun NoneNumeric(attribute: Attribute, userExpression: String) =
        EpisodicCondition(null, attribute, IsNumeric, No, userExpression)

    fun AtLeastNumeric(attribute: Attribute, userExpression: String, count: Int) =
        EpisodicCondition(null, attribute, IsNumeric, AtLeast(count), userExpression)

    fun AtMostNumeric(attribute: Attribute, userExpression: String, count: Int) =
        EpisodicCondition(null, attribute, IsNumeric, AtMost(count), userExpression)
}