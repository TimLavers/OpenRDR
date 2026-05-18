package io.rippledown.caseview

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.ReferenceRange
import io.rippledown.model.Result
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.utils.daysAgo
import io.rippledown.utils.defaultDate
import org.junit.Test

/**
 * Unit tests for [matchesFilter]. The helper governs which attribute rows
 * appear in the case-view body when a filter is active. Behaviour is the
 * single source of truth for the filter's semantics, so each rule has an
 * explicit test.
 */
class AttributeFilteringTest {

    private val haemoglobin = Attribute(1, "HAEMOGLOBIN")
    private val mcv = Attribute(2, "MCV")
    private val ast = Attribute(3, "AST")

    private val case: ViewableCase = run {
        val builder = RDRCaseBuilder()
        builder.addValue(haemoglobin, defaultDate, "194")
        builder.addValue(mcv, defaultDate, "100.2")
        builder.addValue(ast, defaultDate, "23")
        ViewableCase(builder.build("Einstein"), CaseViewProperties(listOf(haemoglobin, mcv, ast)))
    }

    @Test
    fun `blank query matches every attribute`() {
        matchesFilter(case, haemoglobin, "") shouldBe true
        matchesFilter(case, mcv, "   ") shouldBe true
        matchesFilter(case, ast, "\t") shouldBe true
    }

    @Test
    fun `matches attribute name as a substring`() {
        matchesFilter(case, haemoglobin, "HAEM") shouldBe true
        matchesFilter(case, mcv, "MC") shouldBe true
    }

    @Test
    fun `attribute name match is case-insensitive`() {
        matchesFilter(case, haemoglobin, "haemoglobin") shouldBe true
        matchesFilter(case, mcv, "mcv") shouldBe true
        matchesFilter(case, mcv, "Mc") shouldBe true
    }

    @Test
    fun `matches a value text as a substring`() {
        matchesFilter(case, haemoglobin, "194") shouldBe true
        matchesFilter(case, mcv, "100") shouldBe true
    }

    @Test
    fun `value match is case-insensitive`() {
        val builder = RDRCaseBuilder()
        val sex = Attribute(10, "Sex")
        builder.addValue(sex, defaultDate, "Male")
        val c = ViewableCase(builder.build("X"), CaseViewProperties(listOf(sex)))
        matchesFilter(c, sex, "male") shouldBe true
        matchesFilter(c, sex, "MALE") shouldBe true
    }

    @Test
    fun `non-match returns false`() {
        matchesFilter(case, haemoglobin, "thyroid") shouldBe false
        matchesFilter(case, ast, "999") shouldBe false
    }

    @Test
    fun `query is trimmed before matching`() {
        matchesFilter(case, haemoglobin, "   HAEM   ") shouldBe true
    }

    @Test
    fun `matches when any episode value matches even if the latest does not`() {
        val builder = RDRCaseBuilder()
        builder.addValue(haemoglobin, daysAgo(7), "210")
        builder.addValue(haemoglobin, defaultDate, "140")
        val c = ViewableCase(builder.build("Multi"), CaseViewProperties(listOf(haemoglobin)))
        matchesFilter(c, haemoglobin, "210") shouldBe true
        matchesFilter(c, haemoglobin, "140") shouldBe true
    }

    @Test
    fun `attribute not present in case does not throw and returns false for value-only query`() {
        val ghost = Attribute(99, "Ghost")
        matchesFilter(case, ghost, "194") shouldBe false
    }

    @Test
    fun `attribute not present in case still matches by its own name`() {
        val ghost = Attribute(99, "Ghost")
        matchesFilter(case, ghost, "ghost") shouldBe true
    }

    @Test
    fun `matches against reference range bounds`() {
        // A clinician scanning for "130" expects to find HAEMOGLOBIN, whose
        // reference range is 130 - 180, not just rows whose displayed value
        // contains "130".
        val builder = RDRCaseBuilder()
        builder.addResult(
            haemoglobin, defaultDate,
            Result(io.rippledown.model.Value("194"), ReferenceRange("130", "180"), "g/L")
        )
        val c = ViewableCase(builder.build("Einstein"), CaseViewProperties(listOf(haemoglobin)))
        matchesFilter(c, haemoglobin, "130") shouldBe true
        matchesFilter(c, haemoglobin, "180") shouldBe true
    }

    @Test
    fun `matches against units`() {
        val builder = RDRCaseBuilder()
        builder.addResult(
            haemoglobin, defaultDate,
            Result(io.rippledown.model.Value("194"), ReferenceRange("130", "180"), "g/L")
        )
        val c = ViewableCase(builder.build("Einstein"), CaseViewProperties(listOf(haemoglobin)))
        matchesFilter(c, haemoglobin, "g/L") shouldBe true
        matchesFilter(c, haemoglobin, "G/L") shouldBe true
    }

    @Test
    fun `reference range with only an upper bound is searchable`() {
        val builder = RDRCaseBuilder()
        builder.addResult(
            haemoglobin, defaultDate,
            Result(io.rippledown.model.Value("194"), ReferenceRange(null, "180"), null)
        )
        val c = ViewableCase(builder.build("X"), CaseViewProperties(listOf(haemoglobin)))
        matchesFilter(c, haemoglobin, "180") shouldBe true
    }
}
