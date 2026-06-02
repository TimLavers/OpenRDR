package io.rippledown.model

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.containsText
import io.rippledown.model.rule.Rule
import io.rippledown.utils.*
import kotlin.test.Test

class RDRCaseDataTest {
    private val tsh = Attribute(1, "TSH")
    private val abc = Attribute(4, "ABC")
    private val def = Attribute(5, "DEF")
    private val rangeA = ReferenceRange("0.5", "4.0")
    private val rangeB = ReferenceRange("1.0", "5.0")
    private val units1 = "mU/L"
    private val units2 = "x10*6/L"
    private val r1 = Result(Value("0.67"), rangeA, units1)
    private val r2 = Result(Value("0.5"), rangeB, units2)
    private val r3 = Result(Value("0.67"), rangeA, units1)
    private val r4 = Result(Value("0.501"), rangeB, units2)
    private val dateA = daysAgo(3)
    private val e1 = Event(abc, dateA)
    private val e2 = Event(def, dateA)
    private val dateB = daysAgo(1)
    private val e3 = Event(abc, dateB)
    private val e4 = Event(def, dateB)

    @Test
    fun `empty cases have same data` () {
        val empty1 = RDRCase(CaseId(1, "Empty1"), emptyMap())
        val empty2 = RDRCase(CaseId(2, "Empty2"), emptyMap())
        empty1.hasSameDataAs(empty2) shouldBe true
    }

    @Test
    fun `same case data when one case contains the other` () {
        val dataMap1 = mapOf(e1 to r1, e2 to r2, e3 to r3, e4 to r4)
        val dataMap2 = mapOf(e1 to r1, e2 to r2, e3 to r3)
        makeCase(dataMap1).hasSameDataAs(makeCase(dataMap2)) shouldBe false
    }

    @Test
    fun `same attributes and values but different dates` () {
        val data1 = mapOf(e1 to r1, e2 to r2)
        val data2 = mapOf(e1 to r1, e4 to r2)
        makeCase(data1).hasSameDataAs(makeCase(data2)) shouldBe false
    }

    @Test
    fun `same attributes and dates but different values` () {
        val data1 = mapOf(e1 to r1, e2 to r2)
        val data2 = mapOf(e1 to r1, e2 to r4)
        makeCase(data1).hasSameDataAs(makeCase(data2)) shouldBe false
    }

    @Test
    fun `same dates and values but different attributes` () {
        val data1 = mapOf(e1 to r1, e2 to r2)
        val data2 = mapOf(e2 to r1, e1 to r2)
        makeCase(data1).hasSameDataAs(makeCase(data2)) shouldBe false
    }

    @Test
    fun `same data but for units`() {
        val r1Different = r1.copy(units = "U/L")
        val data1 = mapOf(e1 to r1, e2 to r2)
        val data2 = mapOf(e1 to r1Different, e2 to r4)
        makeCase(data1).hasSameDataAs(makeCase(data2)) shouldBe false
    }

    @Test
    fun `same data but for reference ranges`() {
        val changedRange = r1.referenceRange!!.copy(upperString = "4.01")
        val r1Different = r1.copy(referenceRange = changedRange)
        val data1 = mapOf(e1 to r1, e2 to r2)
        val data2 = mapOf(e1 to r1Different, e2 to r4)
        makeCase(data1).hasSameDataAs(makeCase(data2)) shouldBe false
    }

    @Test
    fun `cases with the same data but different names are identical`() {
        val dataMap = mapOf(e1 to r1, e2 to r2, e3 to r3, e4 to r4)
        val case1 = RDRCase(CaseId(1,"Case 1"), dataMap)
        val case2 = RDRCase(CaseId(2,"Case 2"), dataMap)
        case1.hasSameDataAs(case2) shouldBe true
    }

    @Test
    fun `cases with the same data but different ids are identical`() {
        val dataMap = mapOf(e1 to r1, e2 to r2, e3 to r3, e4 to r4)
        val case1 = RDRCase(CaseId(1,"Case 1"), dataMap)
        val case2 = RDRCase(CaseId(2,"Case 1"), dataMap)
        case1.hasSameDataAs(case2) shouldBe true
    }

    @Test
    fun `cases with the same data but different types are identical`() {
        val dataMap = mapOf(e1 to r1, e2 to r2, e3 to r3, e4 to r4)
        val case1 = RDRCase(CaseId(1,"Case 1", CaseType.Processed), dataMap)
        val case2 = RDRCase(CaseId(2,"Case 1", CaseType.Cornerstone), dataMap)
        case1.hasSameDataAs(case2) shouldBe true
    }

    @Test
    fun `cases with the same data but different interpretations are identical`() {
        val dataMap = mapOf(e1 to r1, e2 to r2, e3 to r3, e4 to r4)
        val case1 = makeCase(dataMap)
        val conclusion = Conclusion(9, "Tea is good.")
        val root = Rule(0, null, null, emptySet(), mutableSetOf())
        val conditions = setOf(containsText(100, tsh, "0.667"))
        val rule = Rule(1, root, conclusion, conditions, mutableSetOf())
        case1.interpretation.add(rule)
        val case2 = makeCase(dataMap)
        case1.hasSameDataAs(case2) shouldBe true
    }

    @Test
    fun `a case has the same data as itself`() {
        val dataMap = mapOf(e1 to r1, e2 to r2, e3 to r3, e4 to r4)
        val case = makeCase(dataMap)
        case.hasSameDataAs(case) shouldBe true
    }

    @Test
    fun `hasSameDataAs is symmetric for identical data`() {
        val dataMap = mapOf(e1 to r1, e2 to r2)
        val caseA = makeCase(dataMap)
        val caseB = makeCase(dataMap)
        caseA.hasSameDataAs(caseB) shouldBe true
        caseB.hasSameDataAs(caseA) shouldBe true
    }

    @Test
    fun `hasSameDataAs is symmetric when one case contains the other`() {
        val data1 = mapOf(e1 to r1, e2 to r2, e3 to r3)
        val data2 = mapOf(e1 to r1, e2 to r2)
        makeCase(data1).hasSameDataAs(makeCase(data2)) shouldBe false
        makeCase(data2).hasSameDataAs(makeCase(data1)) shouldBe false
    }

    @Test
    fun `empty case vs non-empty case`() {
        val empty = RDRCase(CaseId(1, "Empty"), emptyMap())
        val nonEmpty = makeCase(mapOf(e1 to r1))
        empty.hasSameDataAs(nonEmpty) shouldBe false
        nonEmpty.hasSameDataAs(empty) shouldBe false
    }

    @Test
    fun `single entry cases with identical data`() {
        val case1 = makeCase(mapOf(e1 to r1))
        val case2 = makeCase(mapOf(e1 to r1))
        case1.hasSameDataAs(case2) shouldBe true
    }

    @Test
    fun `single entry cases with different values`() {
        val case1 = makeCase(mapOf(e1 to r1))
        val case2 = makeCase(mapOf(e1 to r2))
        case1.hasSameDataAs(case2) shouldBe false
    }

    @Test
    fun `same data but for reference range lower bound`() {
        val changedRange = r1.referenceRange!!.copy(lowerString = "0.49")
        val r1Different = r1.copy(referenceRange = changedRange)
        val data1 = mapOf(e1 to r1, e2 to r2)
        val data2 = mapOf(e1 to r1Different, e2 to r2)
        makeCase(data1).hasSameDataAs(makeCase(data2)) shouldBe false
    }

    @Test
    fun `null reference range vs non-null reference range`() {
        val resultWithRange = r1
        val resultWithoutRange = Result(r1.value, null, r1.units)
        val data1 = mapOf(e1 to resultWithRange)
        val data2 = mapOf(e1 to resultWithoutRange)
        makeCase(data1).hasSameDataAs(makeCase(data2)) shouldBe false
    }

    @Test
    fun `null units vs non-null units`() {
        val resultWithUnits = r1
        val resultWithoutUnits = Result(r1.value, r1.referenceRange, null)
        val data1 = mapOf(e1 to resultWithUnits)
        val data2 = mapOf(e1 to resultWithoutUnits)
        makeCase(data1).hasSameDataAs(makeCase(data2)) shouldBe false
    }

    @Test
    fun `both results have null reference range and null units`() {
        val minimal1 = Result("1.0")
        val minimal2 = Result("1.0")
        val data1 = mapOf(e1 to minimal1)
        val data2 = mapOf(e1 to minimal2)
        makeCase(data1).hasSameDataAs(makeCase(data2)) shouldBe true
    }

    @Test
    fun `same number of entries but completely disjoint events`() {
        val data1 = mapOf(e1 to r1, e2 to r2)
        val data2 = mapOf(e3 to r1, e4 to r2)
        makeCase(data1).hasSameDataAs(makeCase(data2)) shouldBe false
    }

    @Test
    fun `same data but different value text`() {
        val result1 = Result(Value("1.0"), rangeA, units1)
        val result2 = Result(Value("1.1"), rangeA, units1)
        val data1 = mapOf(e1 to result1)
        val data2 = mapOf(e1 to result2)
        makeCase(data1).hasSameDataAs(makeCase(data2)) shouldBe false
    }

    private fun makeCase(data: Map<Event, Result>) = RDRCase(CaseId("Whatever"), data)
}
