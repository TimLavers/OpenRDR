package io.rippledown.model.condition

import io.rippledown.model.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal open class ConditionTestBase {

    val tsh = Attribute("TSH")
    val glucose = Attribute("Glucose")
    val clinicalNotes = Attribute("Clinical Notes")
    val range = ReferenceRange("0.50", "4.00")

    fun glucoseOnlyCase(): RDRCase {
        val builder1 = RDRCaseBuilder()
        builder1.addValue(glucose.name, defaultDate,"0.667")
        return builder1.build("Glucose Only")
    }

    fun clinicalNotesCase(notes: String): RDRCase {
        val builder1 = RDRCaseBuilder()
        builder1.addValue(clinicalNotes.name, defaultDate,notes)
        return builder1.build(notes)
    }

    fun highTSHCase(): RDRCase {
        val builder1 = RDRCaseBuilder()
        builder1.addResult(tsh, defaultDate , TestResult("9.667", range, "pmol/L"))
        return builder1.build("HighTSHCase")
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
        builder1.addValue(tsh.name, defaultDate,"0.667")
        return builder1.build("NoTSHRange")
    }

    fun singleEpisodeCaseWithTSHNormal(): RDRCase {
        val builder1 = RDRCaseBuilder()
        builder1.addResult(tsh, defaultDate , TestResult("0.667", range, "pmol/L"))
        return builder1.build("TSHNormal")
    }

    fun twoEpisodeCaseWithBothTSHValuesNormal(): RDRCase {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult(Value("0.67"), range, "mU/L")
        builder.addResult(tsh.name, defaultDate, tshResult1)
        val range0 = ReferenceRange("0.25", "2.90")
        val tshResult0 = TestResult(Value("2.10"), range0, "mU/L")
        val yesterday = daysAgo(1)
        builder.addResult(tsh.name, yesterday, tshResult0)
        return builder.build("Two Episodes")
    }

    fun twoEpisodeCaseWithFirstTSHLowSecondNormal(): RDRCase {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult(Value("0.67"), range, "mU/L")
        builder.addResult(tsh.name, defaultDate, tshResult1)
        val range0 = ReferenceRange("0.25", "2.90")
        val tshResult0 = TestResult(Value("0.08"), range0, "mU/L")
        val yesterday = daysAgo(1)
        builder.addResult(tsh.name, yesterday, tshResult0)
        return builder.build("Two Episodes")
    }

    fun twoEpisodeCaseWithFirstTSHNormalSecondHigh(): RDRCase {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult(Value("4.67"), range, "mU/L")
        builder.addResult(tsh.name, today, tshResult1)
        val range0 = ReferenceRange("0.25", "2.90")
        val tshResult0 = TestResult(Value("1.20"), range0, "mU/L")
        builder.addResult(tsh.name, yesterday, tshResult0)
        return builder.build("Two Episodes")
    }

    fun twoEpisodeCaseWithFirstTSHHighSecondNormal(): RDRCase {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult(Value("1.67"), range, "mU/L")
        builder.addResult(tsh.name, today, tshResult1)
        val range0 = ReferenceRange("0.25", "2.90")
        val tshResult0 = TestResult(Value("5.20"), range0, "mU/L")
        builder.addResult(tsh.name, yesterday, tshResult0)
        return builder.build("Two Episodes")
    }

    fun twoEpisodeCase(attribute: Attribute, firstValue: String, secondValue: String): RDRCase {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult(Value("0.67"), range, "mU/L")
        builder.addResult(tsh.name, defaultDate, tshResult1)
        val range0 = ReferenceRange("0.25", "2.90")
        val tshResult0 = TestResult(Value("0.08"), range0, "mU/L")
        val yesterday = daysAgo(1)
        builder.addResult(tsh.name, yesterday, tshResult0)
        builder.addResult(attribute, yesterday, TestResult(firstValue))
        builder.addResult(attribute, today, TestResult(secondValue))
        return builder.build("Two Episodes")
    }

    fun twoEpisodeCaseWithCurrentTSHValueBlank(): RDRCase {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult("")
        builder.addResult(tsh.name, defaultDate, tshResult1)
        val range0 = ReferenceRange("0.25", "2.90")
        val tshResult0 = TestResult(Value("0.8"), range0, "mU/L")
        val yesterday = daysAgo(1)
        builder.addResult(tsh.name, yesterday, tshResult0)
        return builder.build("Case")
    }

    fun twoEpisodeCaseWithFirstTSHValueBlank(): RDRCase {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult("4.94")
        builder.addResult(tsh.name, defaultDate, tshResult1)
        val range0 = ReferenceRange("0.25", "2.90")
        val tshResult0 = TestResult(Value(""), range0, "mU/L")
        val yesterday = daysAgo(1)
        builder.addResult(tsh.name, yesterday, tshResult0)
        return builder.build("Case")
    }

    fun twoEpisodeCaseWithCurrentTSHValueNonNumeric(): RDRCase {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult("Blah")
        builder.addResult(tsh.name, defaultDate, tshResult1)
        val range0 = ReferenceRange("0.25", "2.90")
        val tshResult0 = TestResult(Value(""), range0, "mU/L")
        val yesterday = daysAgo(1)
        builder.addResult(tsh.name, yesterday, tshResult0)
        return builder.build("Case")
    }

    fun serializeDeserialize(condition: Condition): Condition {
        val serialized = Json.encodeToString(condition)
        return Json.decodeFromString(serialized)
    }
}