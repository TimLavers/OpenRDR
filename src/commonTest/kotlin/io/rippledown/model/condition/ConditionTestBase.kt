package io.rippledown.model.condition

import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

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

    fun serializeDeserialize(isNormal: Condition): Condition {
        val serialized = Json.encodeToString(isNormal)
        return Json.decodeFromString(serialized)
    }
}