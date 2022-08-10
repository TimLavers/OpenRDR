package io.rippledown.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.*
import kotlinx.serialization.json.*

internal class RDRCaseTest {
    private val defaultDate = 90000L

    @Test
    fun getCaseData() {
        val case1 = RDRCase("Case1", emptyMap())
        assertTrue(case1.caseData.isEmpty())
    }

    @Test
    fun addValue() {
        val builder1 = RDRCaseBuilder()
        builder1.addValue("TSH", defaultDate,"0.667")
        val case1 = builder1.build("Case1")
        assertEquals(case1.caseData.size, 1)
        assertEquals(case1.get("TSH")!!.value.text, "0.667")
    }

    @Test
    fun addValueTwice() {
        val builder1 = RDRCaseBuilder()
        builder1.addValue("TSH", defaultDate, "0.67")
        builder1.addValue("TSH", defaultDate, "0.68")
        val case1 = builder1.build("Case1")
        assertEquals(case1.caseData.size, 1)
        assertEquals(case1.get("TSH")!!.value.text, "0.68")
    }

    @Test
    fun addResult() {
        val builder1 = RDRCaseBuilder()
        val tshResult = TestResult(Value("0.67"), ReferenceRange("0.5", "4.0"), "mU/L")
        builder1.addResult("TSH", defaultDate, tshResult)
        val freeT4Result = TestResult(Value("16"), ReferenceRange("10", "20.0"), "pmol/L")
        builder1.addResult("Free T4", defaultDate, freeT4Result)

        val case1 = builder1.build("Case1")
        assertEquals(case1.caseData.size, 2)
        val tshInCase = case1.get("TSH")!!
        assertEquals(tshInCase.value.text, "0.67")
        assertEquals(tshInCase.units, "mU/L")
        assertEquals(tshInCase.referenceRange!!.lower, 0.5F)
        assertEquals(tshInCase.referenceRange!!.upper, 4.0F)

        val freeT4InCase = case1.get("Free T4")!!
        assertEquals(freeT4InCase.value.text, "16")
        assertEquals(freeT4InCase.units, "pmol/L")
        assertEquals(freeT4InCase.referenceRange!!.lower, 10F)
        assertEquals(freeT4InCase.referenceRange!!.upper, 20F)
    }

    @Test
    fun addResultTwice() {
        val builder1 = RDRCaseBuilder()
        val tshResult = TestResult(Value("0.67"), ReferenceRange("0.5", "4.0"), "mU/L")
        builder1.addResult("TSH", defaultDate, tshResult)
        val tshResult2 = TestResult(Value("0.68"), ReferenceRange("0.5", "4.0"), "mU/L")
        builder1.addResult("TSH", defaultDate, tshResult2)
        val case1 = builder1.build("Case1")
        assertEquals(case1.caseData.size, 1)
        val tshInCase = case1.get("TSH")!!
        assertEquals(tshInCase.value.text, "0.68")
    }

    @Test
    fun getName() {
        val case1 = RDRCase("Case1", emptyMap())
        assertEquals(case1.name, "Case1")
    }

    @Test
    fun jsonSerialisation() {
        val case1 = RDRCase("Case1", emptyMap())
        val sd1 = serializeDeserialize(case1)
        assertEquals(sd1, case1)

        val builder2 = RDRCaseBuilder()
        builder2.addValue("TSH", defaultDate, "0.667")
        builder2.addValue("ABC", defaultDate, "6.7")
        val case2 = builder2.build("Case2")
        val sd2 = serializeDeserialize(case2)
        assertEquals(sd2, case2)
        assertEquals(sd2.get("TSH")!!.value.text, "0.667")

        val builder3 = RDRCaseBuilder()
        builder3.addValue("Age", defaultDate, "52")
        val tshResult = TestResult(Value("0.67"), ReferenceRange("0.5", "4.0"), "mU/L")
        builder3.addResult("TSH", defaultDate, tshResult)
        val abcResult = TestResult(Value("0.67"), null, "mU/L")
        builder3.addResult("ABC", defaultDate, abcResult)
        val defResult = TestResult(Value("100"), ReferenceRange("90", "400"),null )
        builder3.addResult("DEF", defaultDate, defResult)
        val case3 = builder3.build("Case3")
        val sd3 = serializeDeserialize(case3)
        assertEquals(sd3, case3)
    }

    private fun serializeDeserialize(rdrCase: RDRCase): RDRCase {
        val format = Json { allowStructuredMapKeys = true }
        val serialized = format.encodeToString(rdrCase)
        return format.decodeFromString(serialized)
    }
}