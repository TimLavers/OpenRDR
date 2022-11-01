package io.rippledown.model.caseview

import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class ViewableCaseTest {
    val abc = Attribute("ABC")
    val tsh = Attribute("TSH")
    private val xyz = Attribute("XYZ")

    @Test
    fun construction() {
        val properties = CaseViewProperties(emptyMap())
        val rdrCase = createCase("Case1")
        val viewableCase = ViewableCase(rdrCase, properties)
        viewableCase.rdrCase shouldBe rdrCase
        viewableCase.viewProperties shouldBe properties
    }

    @Test
    fun name() {
        val properties = CaseViewProperties(emptyMap())
        val viewableCase = ViewableCase(createCase("Case1"), properties)
        viewableCase.name shouldBe "Case1"
    }

    @Test
    fun attributes() {
        val properties = CaseViewProperties(emptyMap())
        val viewableCase = ViewableCase(createCase("Case1"), properties)
        viewableCase.attributes() shouldBe listOf(abc, tsh, xyz)
    }

    @Test
    fun dates() {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult(Value("0.67"), null, "mU/L")
        builder.addResult(tsh.name, defaultDate, tshResult1)
        val tshResult0 = TestResult(Value("0.08"), null, "mU/L")
        val yesterday = daysAgo(1)
        builder.addResult(tsh.name, yesterday, tshResult0)
        val properties = CaseViewProperties(emptyMap())
        val viewableCase = ViewableCase(builder.build("Case1"), properties)
        viewableCase.dates shouldBe listOf(yesterday, defaultDate)
    }

    @Test
    fun serialization() {
        val properties = CaseViewProperties(emptyMap())
        val viewableCase = ViewableCase(createCase("Case1"), properties)
        val format = Json { allowStructuredMapKeys = true }
        val serialized = format.encodeToString(viewableCase)
        val deserialized = format.decodeFromString<ViewableCase>(serialized)
        deserialized shouldBe viewableCase
    }

    private fun createCase(name: String): RDRCase {
        val builder = RDRCaseBuilder()
        builder.addValue(tsh.name, defaultDate, "0.68")
        builder.addValue(xyz.name, defaultDate, "0.66")
        builder.addValue(abc.name, defaultDate, "0.67")
        return builder.build(name)
    }
}