package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.condition.episodic.predicate.*
import io.rippledown.model.condition.episodic.signature.*
import io.rippledown.model.condition.series.Decreasing
import io.rippledown.model.condition.series.Increasing
import io.rippledown.model.condition.structural.IsAbsentFromCase
import io.rippledown.model.condition.structural.IsPresentInCase
import io.rippledown.model.condition.structural.IsSingleEpisodeCase

fun isLow(id: Int? = null, attribute: Attribute) = EpisodicCondition(id, attribute, Low, Current)
fun isNormal(id: Int? = null, attribute: Attribute) = EpisodicCondition(id, attribute, Normal, Current)
fun isHigh(id: Int? = null, attribute: Attribute) = EpisodicCondition(id, attribute, High, Current)
fun isCondition(id: Int? = null, attribute: Attribute, text: String, signature: Signature = Current) =
    EpisodicCondition(id, attribute, Is(text), signature)

fun containsText(id: Int? = null, attribute: Attribute, text: String, signature: Signature = Current) =
    EpisodicCondition(id, attribute, Contains(text), signature)

fun doesNotContainText(id: Int? = null, attribute: Attribute, text: String) =
    EpisodicCondition(id, attribute, DoesNotContain(text), Current)

fun allDoNotContainText(id: Int? = null, attribute: Attribute, text: String) =
    EpisodicCondition(id, attribute, DoesNotContain(text), All)

fun hasCurrentValue(id: Int? = null, attribute: Attribute) = EpisodicCondition(id, attribute, IsNotBlank, Current)
fun hasNoCurrentValue(id: Int? = null, attribute: Attribute) = EpisodicCondition(id, attribute, IsBlank, Current)
fun greaterThanOrEqualTo(id: Int? = null, attribute: Attribute, d: Double) =
    EpisodicCondition(id, attribute, GreaterThanOrEquals(d), Current)

fun lessThanOrEqualTo(id: Int? = null, attribute: Attribute, d: Double) =
    EpisodicCondition(id, attribute, LessThanOrEquals(d), Current)

fun slightlyLow(id: Int? = null, attribute: Attribute, cutoff: Int, signature: Signature = Current) =
    EpisodicCondition(id, attribute, LowByAtMostSomePercentage(cutoff), signature)

fun normalOrSlightlyLow(id: Int? = null, attribute: Attribute, cutoff: Int, signature: Signature = Current) =
    EpisodicCondition(id, attribute, NormalOrLowByAtMostSomePercentage(cutoff), signature)

fun normalOrSlightlyHigh(id: Int? = null, attribute: Attribute, cutoff: Int, signature: Signature = Current) =
    EpisodicCondition(id, attribute, NormalOrHighByAtMostSomePercentage(cutoff), signature)

fun slightlyHigh(id: Int? = null, attribute: Attribute, cutoff: Int, signature: Signature = Current) =
    EpisodicCondition(id, attribute, HighByAtMostSomePercentage(cutoff), signature)

fun isSingleEpisodeCase(id: Int? = null) = CaseStructureCondition(id, IsSingleEpisodeCase)
fun isPresent(attribute: Attribute, id: Int? = null) = CaseStructureCondition(id, IsPresentInCase(attribute))
fun isAbsent(attribute: Attribute, id: Int? = null) = CaseStructureCondition(id, IsAbsentFromCase(attribute))
fun isNumeric(attribute: Attribute, id: Int? = null, signature: Signature = Current) =
    EpisodicCondition(id, attribute, IsNumeric, signature)

fun notNumeric(attribute: Attribute, id: Int? = null, signature: Signature = Current) =
    EpisodicCondition(id, attribute, IsNotNumeric, signature)

fun increasing(attribute: Attribute, id: Int? = null) = SeriesCondition(id, attribute, Increasing)
fun decreasing(attribute: Attribute, id: Int? = null) = SeriesCondition(id, attribute, Decreasing)
fun allNumeric(attribute: Attribute, id: Int? = null) = EpisodicCondition(id, attribute, IsNumeric, All)
fun noneNumeric(attribute: Attribute, id: Int? = null) = EpisodicCondition(id, attribute, IsNumeric, No)
fun atLeastNumeric(count: Int, attribute: Attribute, id: Int? = null) =
    EpisodicCondition(id, attribute, IsNumeric, AtLeast(count))

fun atMostNumeric(count: Int, attribute: Attribute, id: Int? = null) =
    EpisodicCondition(id, attribute, IsNumeric, AtMost(count))
