package io.rippledown.model.condition

import io.kotest.matchers.shouldBe
import io.rippledown.model.*


fun rr(low: String?, high: String?) = ReferenceRange(low, high)
fun tr(value: String) = TestResult(value, null, null)
fun tr(value: String, referenceRange: ReferenceRange) = TestResult(value, referenceRange)
fun tr(value: String, units: String) = TestResult(value, null, units)
fun tr(value: String, referenceRange: ReferenceRange,units: String) = TestResult(value, referenceRange, units)
fun v(value: String) = Value(value)

open class ConditionTestBase {

    val tsh = Attribute(0, "TSH")
    val glucose = Attribute(1, "Glucose")
    val clinicalNotes = Attribute(2, "Clinical Notes")
    val tshRange = ReferenceRange("0.50", "4.00")
    private val attributesById = mapOf(tsh.id to tsh, glucose.id to glucose, clinicalNotes.id to clinicalNotes)

    fun attributeForId(id: Int): Attribute {
        return attributesById[id]!!
    }

    fun glucoseOnlyCase() = glucoseOnlyCase("0.667")

    fun glucoseOnlyCase(value: String): RDRCase {
        val builder1 = RDRCaseBuilder()
        builder1.addValue(glucose, defaultDate,value)
        return builder1.build("Glucose Only")
    }

    fun clinicalNotesCase(notes: String): RDRCase {
        val builder1 = RDRCaseBuilder()
        builder1.addValue(clinicalNotes, defaultDate,notes)
        return builder1.build(notes)
    }

    fun multiEpisodeClinicalNotesCase(vararg notes: String) = multiEpisodeCase(clinicalNotes, *notes)

    fun multiEpisodeTSHCase(vararg values: String) = multiEpisodeCase(tsh, *values)
    fun multiEpisodeGlucoseCase(vararg values: String) = multiEpisodeCase(glucose, *values)

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
        return builder1.build("LowTSHCase")
    }

    fun normalTSHCase(): RDRCase {
        val builder1 = RDRCaseBuilder()
        builder1.addResult(tsh, defaultDate , TestResult("1.30", tshRange, "pmol/L"))
        return builder1.build("NormalTSHCase")
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

    fun checkConditionCanBeSerialized( condition: Condition) {
        val writtenAndRead = serializeDeserialize(condition)
        writtenAndRead shouldBe condition
    }
}