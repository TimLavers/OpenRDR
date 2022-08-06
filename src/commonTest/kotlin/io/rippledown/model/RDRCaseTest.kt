package io.rippledown.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class RDRCaseTest {
    private val defaultDate = 90000L

    @Test
    fun getCaseData() {
        val case1 = RDRCase("Case1")
        assertTrue(case1.caseData.isEmpty())
    }

    @Test
    fun addValue() {
        val case1 = RDRCase("Case1")
        case1.addValue("TSH", "0.667", defaultDate)
        assertEquals(case1.caseData.size, 1)
        assertEquals(case1.latestEpisode()["TSH"]!!.value.text, "0.667")
    }

    @Test
    fun addValueTwice() {
        val case1 = RDRCase("Case1")
        case1.addValue("TSH", "0.67", defaultDate)
        case1.addValue("TSH", "0.68", defaultDate)
        assertEquals(case1.caseData.size, 1)
        assertEquals(case1.latestEpisode()["TSH"]!!.value.text, "0.68")
    }

    @Test
    fun addResult() {
        val case1 = RDRCase("Case1")
        val tshResult = TestResult(Value("0.67"), defaultDate, ReferenceRange("0.5", "4.0"), "mU/L")
        case1.addResult("TSH", tshResult)
        val freeT4Result = TestResult(Value("16"), defaultDate, ReferenceRange("10", "20.0"), "pmol/L")
        case1.addResult("Free T4", freeT4Result)
        assertEquals(case1.caseData.size, 2)
        val tshInCase = case1.latestEpisode()["TSH"]!!
        assertEquals(tshInCase.value.text, "0.67")
        assertEquals(tshInCase.units, "mU/L")
        assertEquals(tshInCase.referenceRange!!.lower, 0.5F)
        assertEquals(tshInCase.referenceRange!!.upper, 4.0F)

        val freeT4InCase = case1.latestEpisode()["Free T4"]!!
        assertEquals(freeT4InCase.value.text, "16")
        assertEquals(freeT4InCase.units, "pmol/L")
        assertEquals(freeT4InCase.referenceRange!!.lower, 10F)
        assertEquals(freeT4InCase.referenceRange!!.upper, 20F)
    }

    @Test
    fun addResultTwice() {
        val case1 = RDRCase("Case1")
        val tshResult = TestResult(Value("0.67"), defaultDate, ReferenceRange("0.5", "4.0"), "mU/L")
        case1.addResult("TSH", tshResult)
        val tshResult2 = TestResult(Value("0.68"), defaultDate, ReferenceRange("0.5", "4.0"), "mU/L")
        case1.addResult("TSH", tshResult2)
        assertEquals(case1.caseData.size, 1)
        val tshInCase = case1.latestEpisode()["TSH"]!!
        assertEquals(tshInCase.value.text, "0.68")
    }

    @Test
    fun getName() {
        val case1 = RDRCase("Case1")
        assertEquals(case1.name, "Case1")
    }

    @Test
    fun jsonSerialisation() {
        val case1 = RDRCase("Case1")
        val sd1 = serializeDeserialize(case1)
        assertEquals(sd1, case1)

        val case2 = RDRCase("Case2")
        case2.addValue("TSH", "0.667", defaultDate)
        case2.addValue("ABC", "6.7", defaultDate)
        val sd2 = serializeDeserialize(case2)
        assertEquals(sd2, case2)
        assertEquals(sd2.latestEpisode()["TSH"]!!.value.text, "0.667")

        val case3 = RDRCase("Case3")
        case3.addValue("Age", "52", defaultDate)
        val tshResult = TestResult(Value("0.67"), defaultDate, ReferenceRange("0.5", "4.0"), "mU/L")
        case3.addResult("TSH", tshResult)
        val abcResult = TestResult(Value("0.67"), defaultDate, null, "mU/L")
        case3.addResult("ABC", abcResult)
        val defResult = TestResult(Value("100"), defaultDate, ReferenceRange("90", "400"),null )
        case3.addResult("DEF", defResult)
        val sd3 = serializeDeserialize(case3)
        assertEquals(sd3, case3)
    }

    private fun serializeDeserialize(rdrCase: RDRCase): RDRCase {
        val serialized = Json.encodeToString(rdrCase)
        return Json.decodeFromString(serialized)
    }
}