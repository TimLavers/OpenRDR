package io.rippledown.model.condition

import io.rippledown.model.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

open class ConditionTestBase {

    val tsh = Attribute(0, "TSH")
    val glucose = Attribute(1, "Glucose")
    val clinicalNotes = Attribute(2, "Clinical Notes")
    val range = ReferenceRange("0.50", "4.00")
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

    fun highTSHCase(): RDRCase {
        val builder1 = RDRCaseBuilder()
        builder1.addResult(tsh, defaultDate , TestResult("9.667", range, "pmol/L"))
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
        builder1.addResult(tsh, defaultDate , TestResult("0.30", range, "pmol/L"))
        return builder1.build("HighTSHCase")
    }

    fun tshValueNonNumericCase(): RDRCase {
        val builder1 = RDRCaseBuilder()
        builder1.addResult(tsh, defaultDate , TestResult("n.a.", range, "pmol/L"))
        return builder1.build("TSHValueNonNumeric")
    }

    fun tshValueHasNoRangeCase(): RDRCase {
        val builder1 = RDRCaseBuilder()
        builder1.addValue(tsh, defaultDate,"0.667")
        return builder1.build("NoTSHRange")
    }

    fun singleEpisodeCaseWithTSHNormal(): RDRCase {
        val builder1 = RDRCaseBuilder()
        builder1.addResult(tsh, defaultDate , TestResult("0.667", range, "pmol/L"))
        return builder1.build("TSHNormal")
    }

    fun singleEpisodeCaseWithTSHAsGiven(value: String): RDRCase {
        val builder1 = RDRCaseBuilder()
        builder1.addResult(tsh, defaultDate , TestResult(value))
        return builder1.build("TSHCase")
    }

    fun twoEpisodeCaseWithBothTSHValuesNormal(): RDRCase {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult(Value("0.67"), range, "mU/L")
        builder.addResult(tsh, defaultDate, tshResult1)
        val range0 = ReferenceRange("0.25", "2.90")
        val tshResult0 = TestResult(Value("2.10"), range0, "mU/L")
        val yesterday = daysAgo(1)
        builder.addResult(tsh, yesterday, tshResult0)
        return builder.build("Two Episodes")
    }

    fun twoEpisodeCaseWithFirstTSHLowSecondNormal(): RDRCase {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult(Value("0.67"), range, "mU/L")
        builder.addResult(tsh, defaultDate, tshResult1)
        val range0 = ReferenceRange("0.25", "2.90")
        val tshResult0 = TestResult(Value("0.08"), range0, "mU/L")
        val yesterday = daysAgo(1)
        builder.addResult(tsh, yesterday, tshResult0)
        return builder.build("Two Episodes")
    }

    fun twoEpisodeCaseWithFirstTSHNormalSecondHigh(): RDRCase {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult(Value("4.67"), range, "mU/L")
        builder.addResult(tsh, today, tshResult1)
        val range0 = ReferenceRange("0.25", "2.90")
        val tshResult0 = TestResult(Value("1.20"), range0, "mU/L")
        builder.addResult(tsh, yesterday, tshResult0)
        return builder.build("Two Episodes")
    }

    fun twoEpisodeCaseWithFirstTSHNormalSecondLow(): RDRCase {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult(Value("0.05"), range, "mU/L")
        builder.addResult(tsh, today, tshResult1)
        val range0 = ReferenceRange("0.25", "2.90")
        val tshResult0 = TestResult(Value("1.20"), range0, "mU/L")
        builder.addResult(tsh, yesterday, tshResult0)
        return builder.build("Two Episodes")
    }

    fun twoEpisodeCaseWithFirstTSHHighSecondNormal(): RDRCase {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult(Value("1.67"), range, "mU/L")
        builder.addResult(tsh, today, tshResult1)
        val range0 = ReferenceRange("0.25", "2.90")
        val tshResult0 = TestResult(Value("5.20"), range0, "mU/L")
        builder.addResult(tsh, yesterday, tshResult0)
        return builder.build("Two Episodes")
    }

    fun twoEpisodeCase(attribute: Attribute, firstValue: String, secondValue: String): RDRCase {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult(Value("0.67"), range, "mU/L")
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
}