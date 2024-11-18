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

    fun DoesNotContain(attribute: Attribute, userExpression: String, text: String) = EpisodicCondition(
        null, attribute, DoesNotContain(text), Current, userExpression
    )

    fun GreaterThanOrEqualTo(attribute: Attribute, userExpression: String, d: String) =
        EpisodicCondition(null, attribute, GreaterThanOrEquals(d.toDoubleOrNull()!!), Current, userExpression)

    fun LessThanOrEqualTo(attribute: Attribute, userExpression: String, d: String) =
        EpisodicCondition(null, attribute, LessThanOrEquals(d.toDoubleOrNull()!!), Current, userExpression)

    //TODO
    fun slightlyLow(attribute: Attribute, userExpression: String, cutoff: Int, signature: Signature = Current) =
        EpisodicCondition(null, attribute, LowByAtMostSomePercentage(cutoff), signature)

    fun normalOrSlightlyLow(attribute: Attribute, userExpression: String, cutoff: Int, signature: Signature = Current) =
        EpisodicCondition(null, attribute, NormalOrLowByAtMostSomePercentage(cutoff), signature)

    fun normalOrSlightlyHigh(
        attribute: Attribute,
        userExpression: String,
        cutoff: Int,
        signature: Signature = Current
    ) =
        EpisodicCondition(null, attribute, NormalOrHighByAtMostSomePercentage(cutoff), signature)

    fun slightlyHigh(attribute: Attribute, userExpression: String, cutoff: Int, signature: Signature = Current) =
        EpisodicCondition(null, attribute, HighByAtMostSomePercentage(cutoff), signature)

    fun isSingleEpisodeCase(id: Int? = null) = CaseStructureCondition(null, IsSingleEpisodeCase, "")
    fun allDoNotContainText(attribute: Attribute, userExpression: String, text: String) =
        EpisodicCondition(null, attribute, DoesNotContain(text), All)

    fun allNumeric(attribute: Attribute, userExpression: String) = EpisodicCondition(null, attribute, IsNumeric, All)
    fun noneNumeric(attribute: Attribute, userExpression: String) = EpisodicCondition(null, attribute, IsNumeric, No)
    fun atLeastNumeric(count: Int, attribute: Attribute, userExpression: String) =
        EpisodicCondition(null, attribute, IsNumeric, AtLeast(count))

    fun atMostNumeric(count: Int, attribute: Attribute, userExpression: String) =
        EpisodicCondition(null, attribute, IsNumeric, AtMost(count))
}