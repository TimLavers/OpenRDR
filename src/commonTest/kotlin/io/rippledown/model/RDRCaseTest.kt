package io.rippledown.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.rippledown.model.condition.ContainsText
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.DiffList
import io.rippledown.model.rule.Rule
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RDRCaseTest {
    private val tsh = Attribute("TSH")
    private val tshRange = ReferenceRange("0.5", "4.0")
    private val ft4 = Attribute("FT4")
    private val ft4Range = ReferenceRange("0.25", "2.90")

    @Test
    fun getCaseData() {
        val case1 = RDRCase("Case1", emptyMap())
        assertTrue(case1.dates.isEmpty())
        assertTrue(case1.data.isEmpty())
    }

    @Test
    fun addValue() {
        val builder1 = RDRCaseBuilder()
        builder1.addValue("TSH", defaultDate, "0.667")
        val case1 = builder1.build("Case1")
        assertEquals(1, case1.dates.size)
        assertEquals("0.667", case1.get("TSH")!!.value.text)
    }

    @Test
    fun addValueTwice() {
        val builder1 = RDRCaseBuilder()
        builder1.addValue("TSH", defaultDate, "0.67")
        builder1.addValue("TSH", defaultDate, "0.68")
        val case1 = builder1.build("Case1")
        assertEquals(case1.dates.size, 1)
        assertEquals("0.68", case1.get("TSH")!!.value.text)
    }

    @Test
    fun addResult() {
        val builder1 = RDRCaseBuilder()
        val tshResult = TestResult(Value("0.67"), ReferenceRange("0.5", "4.0"), "mU/L")
        builder1.addResult("TSH", defaultDate, tshResult)
        val freeT4Result = TestResult(Value("16"), ReferenceRange("10", "20.0"), "pmol/L")
        builder1.addResult("Free T4", defaultDate, freeT4Result)

        val case1 = builder1.build("Case1")
        assertEquals(2, case1.data.size)
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
    fun oneAttributeTwoEpisodes() {
        val builder = RDRCaseBuilder()
        val range1 = ReferenceRange("0.5", "4.0")
        val tshResult1 = TestResult(Value("0.67"), range1, "mU/L")
        builder.addResult(tsh.name, defaultDate, tshResult1)
        val range0 = ReferenceRange("0.25", "2.90")
        val tshResult0 = TestResult(Value("0.08"), range0, "mU/L")
        val yesterday = daysAgo(1)
        builder.addResult(tsh.name, yesterday, tshResult0)
        val case = builder.build("Case1")
        assertEquals(2, case.dates.size)
        assertEquals(yesterday, case.dates[0])
        assertEquals(defaultDate, case.dates[1])

        assertEquals(case.getLatest(tsh)!!.value, Value("0.67"))
        assertEquals(case.getLatest(tsh)!!.referenceRange, range1)

        checkValues(case, tsh, "0.08", "0.67")
    }

    @Test
    fun dates() {
        assertEquals(0, RDRCase("Empty", emptyMap()).dates.size)

        val builder1 = RDRCaseBuilder()
        builder1.addResult(tsh, yesterday, TestResult("9.4"))
        assertEquals(1, builder1.build("1").dates.size)
        assertEquals(yesterday, builder1.build("1").dates[0])

        val builder2 = RDRCaseBuilder()
        builder2.addResult(tsh, yesterday, TestResult("9.4"))
        builder2.addResult(tsh, lastWeek, TestResult("9.4"))
        builder2.addResult(tsh, today, TestResult("9.9"))
        val case2 = builder2.build("1")
        assertEquals(3, case2.dates.size)
        assertEquals(lastWeek, case2.dates[0])
        assertEquals(yesterday, case2.dates[1])
        assertEquals(today, case2.dates[2])
    }

    @Test
    fun attributes() {
        assertEquals(0, RDRCase("Empty", emptyMap()).attributes.size)

        val builder1 = RDRCaseBuilder()
        builder1.addResult(tsh, yesterday, TestResult("9.4"))
        assertEquals(1, builder1.build("1").attributes.size)
        assertEquals(tsh, builder1.build("1").attributes.first())

        val builder2 = RDRCaseBuilder()
        builder2.addResult(tsh, yesterday, TestResult("9.4"))
        builder2.addResult(tsh, lastWeek, TestResult("9.4"))
        builder2.addResult(tsh, today, TestResult("9.9"))
        val case2 = builder2.build("2")
        assertEquals(1, case2.attributes.size)
        assertEquals(tsh, case2.attributes.first())

        val ft4Result = TestResult(Value("0.08"), ft4Range, "mU/L")
        builder2.addResult(ft4, yesterday, ft4Result)
        val case3 = builder2.build("3")
        assertEquals(2, case3.attributes.size)
        assertTrue(case3.attributes.contains(tsh))
        assertTrue(case3.attributes.contains(ft4))
    }

    @Test
    fun twoAttributesWithSamplesOnDifferentDates() {
        val tshResult = TestResult(Value("0.67"), tshRange, "mU/L")
        val ft4Result = TestResult(Value("0.08"), ft4Range, "mU/L")

        val builder = RDRCaseBuilder()
        builder.addResult(tsh.name, defaultDate, tshResult)
        val yesterday = daysAgo(1)
        builder.addResult(ft4.name, yesterday, ft4Result)

        val case = builder.build("Case1")
        val datesInCase = case.dates
        assertEquals(datesInCase.size, 2)
        assertEquals(datesInCase[0], yesterday)
        assertEquals(datesInCase[1], defaultDate)

        assertEquals(case.getLatest(tsh)!!.value, Value("0.67"))
        assertEquals(case.getLatest(tsh)!!.referenceRange, tshRange)

        assertEquals(case.getLatest(ft4)!!.value, Value(""))
        assertEquals(case.getLatest(ft4)!!.referenceRange, null)

        checkValues(case, tsh, "", "0.67")
        checkValues(case, ft4, "0.08", "")

        val ft4Values = case.resultsFor(ft4)
        assertEquals(2, ft4Values!!.size)
        assertEquals("0.08", ft4Values[0].value.text)
        assertEquals("", ft4Values[1].value.text)
        assertEquals("mU/L", ft4Values.units)
    }

    @Test
    fun threeAttributesThreeDatesDenseCase() {
        val d0 = daysAgo(3)
        val d1 = daysAgo(2)
        val d2 = daysAgo(1)
        val builder = RDRCaseBuilder()
        builder.addValue("A", d0, "A1")
        builder.addValue("A", d1, "A2")
        builder.addValue("A", d2, "A3")
        builder.addValue("B", d0, "B1")
        builder.addValue("B", d1, "B2")
        builder.addValue("B", d2, "B3")
        builder.addValue("C", d0, "C1")
        builder.addValue("C", d1, "C2")
        builder.addValue("C", d2, "C3")

        val case = builder.build("Case1")
        val datesInCase = case.dates
        assertEquals(datesInCase.size, 3)
        assertEquals(datesInCase[0], d0)
        assertEquals(datesInCase[1], d1)
        assertEquals(datesInCase[2], d2)

        checkValues(case, "A", "A1", "A2", "A3")
        checkValues(case, "B", "B1", "B2", "B3")
        checkValues(case, "C", "C1", "C2", "C3")

        // Check serialisation.
        val sd = serializeDeserialize(case)
        assertEquals(sd, case)
    }

    @Test
    fun addResultTwice() {
        val builder1 = RDRCaseBuilder()
        val tshResult = TestResult(Value("0.67"), ReferenceRange("0.5", "4.0"), "mU/L")
        builder1.addResult("TSH", defaultDate, tshResult)
        val tshResult2 = TestResult(Value("0.68"), ReferenceRange("0.5", "4.0"), "mU/L")
        builder1.addResult("TSH", defaultDate, tshResult2)
        val case1 = builder1.build("Case1")
        assertEquals(1, case1.dates.size)
        val tshInCase = case1.get("TSH")!!
        assertEquals(tshInCase.value.text, "0.68")
    }

    @Test
    fun getName() {
        val case1 = RDRCase("Case1")
        assertEquals(case1.name, "Case1")
    }

    @Test
    fun interpretation() {
        val case = basicCase()
        case.interpretation.caseId.name shouldBe case.name
        case.interpretation.caseId.id shouldBe case.name

        case.interpretation.conclusions().size shouldBe 0
    }

    @Test
    fun resetInterpretation() {
        val case = basicCase()
        val originalInterpretation = case.interpretation

        case.resetInterpretation()
        case.interpretation.caseId.name shouldBe case.name
        case.interpretation.caseId.id shouldBe case.name
        case.interpretation.conclusions().size shouldBe 0
        case.interpretation shouldBeSameInstanceAs originalInterpretation
    }

    @Test
    fun serializedWithInterpretation() {
        val conclusion = Conclusion("Tea is good.")
        val root = Rule("root", null, null, emptySet(), mutableSetOf())
        val conditions = setOf(ContainsText(tsh, "0.667"))
        val rule = Rule("r", root, conclusion, conditions, mutableSetOf())
        val case = RDRCase()
        case.interpretation.add(rule)
        case.interpretation.conclusions().first() shouldBe conclusion

        val sd = serializeDeserialize(case)
        sd shouldBe case
        sd.interpretation shouldBe case.interpretation
        sd.interpretation.conclusions().first() shouldBe conclusion
    }

    @Test
    fun serializedWithInterpretation1() {
        val conclusion = Conclusion("Tea is good.")
        val root = Rule("root", null, null, emptySet(), mutableSetOf())
        val conditions = setOf(ContainsText(tsh, "0.667"))
        val rule = Rule("r", root, conclusion, conditions, mutableSetOf())
        val case = RDRCase()
        case.interpretation = Interpretation(CaseId()).apply { add(rule) }

        val sd = serializeDeserialize(case)
        sd shouldBe case
        sd.interpretation.conclusions().first() shouldBe conclusion
    }

    @Test
    @Ignore //TODO: fix
    fun serializedWithDiffList() {
        val case = basicCase()
        val diffList = DiffList(listOf(Addition("Coffee is very good")))
        case.interpretation.diffList = diffList

        val sd = serializeDeserialize(case)
        sd shouldBe case
        sd.interpretation.diffList shouldBe diffList
    }

    @Test
    @Ignore //TODO: fix
    fun serializedWithVerifiedText() {
        val case = basicCase()
        val text = "Coffee is very good"
        case.interpretation.verifiedText = text

        val sd = serializeDeserialize(case)
        sd shouldBe case
        sd.interpretation.verifiedText shouldBe text
    }

    @Test
    fun serialisation() {
        val case = RDRCase("Case")
        val sd = serializeDeserialize(case)
        sd shouldBe case
    }

    @Test
    fun serialisationWithAttribute() {
        val builder = RDRCaseBuilder()
        builder.addValue("TSH", defaultDate, "0.667")
        builder.addValue("ABC", defaultDate, "6.7")
        val case = builder.build("Case")
        val sd = serializeDeserialize(case)
        sd shouldBe case
        sd.get("TSH")!!.value.text shouldBe "0.667"
        sd.get("ABC")!!.value.text shouldBe "6.7"
    }


    @Test
    fun serialisationWithReferenceRange() {
        val tshResult = TestResult(Value("0.67"), ReferenceRange("0.5", "4.0"), "mU/L")
        val abcResult = TestResult(Value("0.87"), null, "mg/dl")
        val defResult = TestResult(Value("100"), ReferenceRange("90", "400"), null)
        val case = with(RDRCaseBuilder()) {
            addValue("Age", defaultDate, "52")
            addResult("TSH", defaultDate, tshResult)
            addResult("ABC", defaultDate, abcResult)
            addResult("DEF", defaultDate, defResult)
            build("Case")
        }
        val sd = serializeDeserialize(case)
        sd shouldBe case
        with(sd) {
            get("Age")!!.value.text shouldBe "52"
            with(get("TSH")!!) {
                value.text shouldBe "0.67"
                referenceRange!!.lower shouldBe "0.5".toFloat()
                referenceRange!!.upper shouldBe "4.0".toFloat()
                units shouldBe "mU/L"
            }
            with(get("ABC")!!) {
                value.text shouldBe "0.87"
                referenceRange shouldBe null
                units shouldBe "mg/dl"
            }
            with(get("DEF")!!) {
                value.text shouldBe "100"
                referenceRange!!.lower shouldBe "90".toFloat()
                referenceRange!!.upper shouldBe "400".toFloat()
                units shouldBe null
            }
        }
    }
}

private fun basicCase(): RDRCase {
    val builder1 = RDRCaseBuilder()
    builder1.addValue("TSH", defaultDate, "0.667")
    return builder1.build("Case1")
}

private fun serializeDeserialize(rdrCase: RDRCase): RDRCase {
    val format = Json {
        prettyPrint = true
        allowStructuredMapKeys = true
    }
    val serialized = format.encodeToString(rdrCase)
    return format.decodeFromString(serialized)
}

private fun checkValues(case: RDRCase, attribute: Attribute, vararg expectedValues: String) {
    checkValues(case, attribute.name, expectedValues = expectedValues)
}

private fun checkValues(case: RDRCase, attributeName: String, vararg expectedValues: String) {
    val inCase = case.values(attributeName)!!
    assertEquals(expectedValues.size, inCase.size)
    inCase.zip(expectedValues).forEach {
        assertEquals(it.first.value.text, it.second)
    }
}

