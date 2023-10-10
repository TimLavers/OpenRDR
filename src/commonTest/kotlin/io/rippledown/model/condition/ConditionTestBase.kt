package io.rippledown.model.condition

import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import io.rippledown.model.condition.tabular.chain.Current
import io.rippledown.model.condition.tabular.predicate.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun isLow(id: Int? = null,attribute: Attribute) = TabularCondition(id, attribute, Low, Current)
fun isNormal(id: Int? = null,attribute: Attribute) = TabularCondition(id, attribute, Normal, Current)
fun isHigh(id: Int? = null, attribute: Attribute) = TabularCondition(id, attribute, High, Current)
fun isCondition(id: Int? = null,attribute: Attribute, text: String) = TabularCondition(id, attribute, Is(text), Current)
fun containsText(id: Int? = null, attribute: Attribute, text: String) = TabularCondition(id, attribute, Contains(text), Current)
fun hasCurrentValue(id: Int? = null, attribute: Attribute) = TabularCondition(id, attribute, IsNotBlank, Current)
fun hasNoCurrentValue(id: Int? = null, attribute: Attribute) = TabularCondition(id, attribute, IsBlank, Current)
fun greaterThanOrEqualTo(id: Int? = null,attribute: Attribute, d: Double) = TabularCondition(id, attribute, GreaterThanOrEquals(d), Current)
fun lessThanOrEqualTo(id: Int? = null, attribute: Attribute, d: Double) = TabularCondition(id, attribute, LessThanOrEquals(d), Current)
fun slightlyLow(id: Int? = null, attribute: Attribute, cutoff: Int) = TabularCondition(id, attribute, HighByAtMostSomePercentage(cutoff), Current)

open class ConditionTestBase {

    val tsh = Attribute(0, "TSH")
    val glucose = Attribute(1, "Glucose")
    val clinicalNotes = Attribute(2, "Clinical Notes")
    val tshRange = ReferenceRange("0.50", "4.00")
    private val attributesById = mapOf(tsh.id to tsh, glucose.id to glucose, clinicalNotes.id to clinicalNotes)

    fun attributeForId(id: Int): Attribute {
        return attributesById[id]!!
    }

    fun glucoseOnlyCase(): RDRCase {
        val builder1 = RDRCaseBuilder()
        builder1.addValue(glucose, defaultDate,"0.667")
        return builder1.build("Glucose Only")
    }

    fun clinicalNotesCase(notes: String): RDRCase {
        val builder1 = RDRCaseBuilder()
        builder1.addValue(clinicalNotes, defaultDate,notes)
        return builder1.build(notes)
    }

    fun multiEpisodeClinicalNotesCase(vararg notes: String) = multiEpisodeCase(clinicalNotes, *notes)

    fun multiEpisodeTSHCase(vararg values: String) = multiEpisodeCase(tsh, *values)

    private fun multiEpisodeCase(attribute: Attribute, vararg values: String): RDRCase {
        val builder1 = RDRCaseBuilder()
        values.withIndex().forEach {
            val date = daysAfter(it.index)
            builder1.addValue(attribute, date ,it.value)
        }
        return builder1.build("A Case")
    }

    fun highTSHCase(): RDRCase {
        val builder1 = RDRCaseBuilder()
        builder1.addResult(tsh, defaultDate , TestResult("9.667", tshRange, "pmol/L"))
        return builder1.build("HighTSHCase")
    }

    fun highTSHWithOneSidedRangeCase(): RDRCase {
        val builder1 = RDRCaseBuilder()
        val range = ReferenceRange(null, "6.0")
        builder1.addResult(tsh, defaultDate , TestResult("9.667", range, "pmol/L"))
        return builder1.build("HighTSHWithOneSidedRangeCase")
    }

    fun lowTSHCase(): RDRCase {
        val builder1 = RDRCaseBuilder()
        builder1.addResult(tsh, defaultDate , TestResult("0.30", tshRange, "pmol/L"))
        return builder1.build("HighTSHCase")
    }

    fun tshValueNonNumericCase(): RDRCase {
        val builder1 = RDRCaseBuilder()
        builder1.addResult(tsh, defaultDate , TestResult("n.a.", tshRange, "pmol/L"))
        return builder1.build("TSHValueNonNumeric")
    }

    fun tshValueHasNoRangeCase(): RDRCase {
        val builder1 = RDRCaseBuilder()
        builder1.addValue(tsh, defaultDate,"0.667")
        return builder1.build("NoTSHRange")
    }

    fun singleEpisodeCaseWithTSHNormal(): RDRCase {
        val builder1 = RDRCaseBuilder()
        builder1.addResult(tsh, defaultDate , TestResult("0.667", tshRange, "pmol/L"))
        return builder1.build("TSHNormal")
    }

    fun singleEpisodeCaseWithTSHAsGiven(value: String): RDRCase {
        val builder1 = RDRCaseBuilder()
        builder1.addResult(tsh, defaultDate , TestResult(value))
        return builder1.build("TSHCase")
    }

    fun twoEpisodeCaseWithBothTSHValuesNormal(): RDRCase {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult(Value("0.67"), tshRange, "mU/L")
        builder.addResult(tsh, defaultDate, tshResult1)
        val range0 = ReferenceRange("0.25", "2.90")
        val tshResult0 = TestResult(Value("2.10"), range0, "mU/L")
        val yesterday = daysAgo(1)
        builder.addResult(tsh, yesterday, tshResult0)
        return builder.build("Two Episodes")
    }

    fun twoEpisodeCaseWithFirstTSHLowSecondNormal(): RDRCase {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult(Value("0.67"), tshRange, "mU/L")
        builder.addResult(tsh, defaultDate, tshResult1)
        val range0 = ReferenceRange("0.25", "2.90")
        val tshResult0 = TestResult(Value("0.08"), range0, "mU/L")
        val yesterday = daysAgo(1)
        builder.addResult(tsh, yesterday, tshResult0)
        return builder.build("Two Episodes")
    }

    fun twoEpisodeCaseWithFirstTSHNormalSecondHigh(): RDRCase {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult(Value("4.67"), tshRange, "mU/L")
        builder.addResult(tsh, today, tshResult1)
        val range0 = ReferenceRange("0.25", "2.90")
        val tshResult0 = TestResult(Value("1.20"), range0, "mU/L")
        builder.addResult(tsh, yesterday, tshResult0)
        return builder.build("Two Episodes")
    }

    fun twoEpisodeCaseWithFirstTSHNormalSecondLow(): RDRCase {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult(Value("0.05"), tshRange, "mU/L")
        builder.addResult(tsh, today, tshResult1)
        val range0 = ReferenceRange("0.25", "2.90")
        val tshResult0 = TestResult(Value("1.20"), range0, "mU/L")
        builder.addResult(tsh, yesterday, tshResult0)
        return builder.build("Two Episodes")
    }

    fun threeEpisodeCaseWithEachTshLow(): RDRCase {
        val builder = RDRCaseBuilder()
        val tshResult2 = TestResult(Value("0.08"), tshRange, "mU/L")
        builder.addResult(tsh, today, tshResult2)
        val tshResult1 = TestResult(Value("0.05"), tshRange, "mU/L")
        builder.addResult(tsh, yesterday, tshResult1)
        val range0 = ReferenceRange("0.25", "2.90")
        val tshResult0 = TestResult(Value("0.20"), range0, "mU/L")
        builder.addResult(tsh, lastWeek, tshResult0)
        return builder.build("Two Episodes")
    }

    fun twoEpisodeCaseWithFirstTSHHighSecondNormal(): RDRCase {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult(Value("1.67"), tshRange, "mU/L")
        builder.addResult(tsh, today, tshResult1)
        val range0 = ReferenceRange("0.25", "2.90")
        val tshResult0 = TestResult(Value("5.20"), range0, "mU/L")
        builder.addResult(tsh, yesterday, tshResult0)
        return builder.build("Two Episodes")
    }

    fun twoEpisodeCase(attribute: Attribute, firstValue: String, secondValue: String): RDRCase {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult(Value("0.67"), tshRange, "mU/L")
        builder.addResult(tsh, defaultDate, tshResult1)
        val range0 = ReferenceRange("0.25", "2.90")
        val tshResult0 = TestResult(Value("0.08"), range0, "mU/L")
        val yesterday = daysAgo(1)
        builder.addResult(tsh, yesterday, tshResult0)
        builder.addResult(attribute, yesterday, TestResult(firstValue))
        builder.addResult(attribute, today, TestResult(secondValue))
        return builder.build("Two Episodes")
    }

    fun twoEpisodeCaseWithCurrentTSHValueBlank(): RDRCase {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult("")
        builder.addResult(tsh, defaultDate, tshResult1)
        val range0 = ReferenceRange("0.25", "2.90")
        val tshResult0 = TestResult(Value("0.8"), range0, "mU/L")
        val yesterday = daysAgo(1)
        builder.addResult(tsh, yesterday, tshResult0)
        return builder.build("Case")
    }

    fun twoEpisodeCaseWithFirstTSHValueBlank(): RDRCase {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult("4.94")
        builder.addResult(tsh, defaultDate, tshResult1)
        val range0 = ReferenceRange("0.25", "2.90")
        val tshResult0 = TestResult(Value(""), range0, "mU/L")
        val yesterday = daysAgo(1)
        builder.addResult(tsh, yesterday, tshResult0)
        return builder.build("Case")
    }

    fun twoEpisodeCaseWithCurrentTSHValueNonNumeric(): RDRCase {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult("Blah")
        builder.addResult(tsh, defaultDate, tshResult1)
        val range0 = ReferenceRange("0.25", "2.90")
        val tshResult0 = TestResult(Value(""), range0, "mU/L")
        val yesterday = daysAgo(1)
        builder.addResult(tsh, yesterday, tshResult0)
        return builder.build("Case")
    }

    fun serializeDeserialize(condition: Condition): Condition {
        val serialized = Json.encodeToString(condition)
        return Json.decodeFromString(serialized)
    }

    fun serializeDeserialize(condition: TabularCondition): TabularCondition {
        val serialized = Json.encodeToString(condition)
        return Json.decodeFromString(serialized)
    }

    fun serializeDeserialize(condition: SeriesCondition): SeriesCondition = Json.decodeFromString(Json.encodeToString(condition))

    fun checkConditionCanBeSerialized( condition: Condition) {
        val writtenAndRead = serializeDeserialize(condition)
        writtenAndRead shouldBe condition
    }
}