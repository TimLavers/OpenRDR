package io.rippledown.model.caseview

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import io.rippledown.utils.*
import kotlinx.serialization.json.Json
import kotlin.test.Test

class ViewableCaseTest {
    val abc = Attribute(1, "ABC")
    val tsh = Attribute(2, "TSH")
    private val xyz = Attribute(3, "XYZ")

    @Test
    fun construction() {
        val rdrCase = createCase("Case1")
        val viewableCase = ViewableCase(rdrCase, caseViewProperties())
        viewableCase.case shouldBe rdrCase
        viewableCase.viewProperties shouldBe caseViewProperties()
    }

    @Test
    fun name() {
        ViewableCase(createCase("Case1"), caseViewProperties()).name shouldBe "Case1"
    }

    @Test
    fun attributes() {
        val properties = caseViewProperties()
        val viewableCase = ViewableCase(createCase("Case1"), properties)
        viewableCase.attributes() shouldBe listOf(abc, tsh, xyz)
    }

    @Test
    fun dates() {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult(Value("0.67"), null, "mU/L")
        builder.addResult(tsh, defaultDate, tshResult1)
        val tshResult0 = TestResult(Value("0.08"), null, "mU/L")
        val yesterday = daysAgo(1)
        builder.addResult(tsh, yesterday, tshResult0)
        val properties = CaseViewProperties(listOf(tsh))
        val viewableCase = ViewableCase(builder.build("Case1"), properties)
        viewableCase.dates shouldBe listOf(yesterday, defaultDate)
    }

   @Test
    fun numberOfColumns() {
        val builder = RDRCaseBuilder()
        builder.addValue(tsh, defaultDate, "1.0")
        builder.addValue(tsh, daysAgo(1), "2.0")
        val properties = CaseViewProperties(listOf(tsh))
        val viewableCase = ViewableCase(builder.build("Case1"), properties)
        viewableCase.numberOfColumns shouldBe 2
    }

    @Test
    fun serialization() {
        val properties = caseViewProperties()
        val viewableCase = ViewableCase(createCase("Case1"), properties)
        val format = Json { allowStructuredMapKeys = true }
        val serialized = format.encodeToString(viewableCase)
        val deserialized = format.decodeFromString<ViewableCase>(serialized)
        deserialized shouldBe viewableCase
    }

    @Test
    fun serializationWithInterpretation() {
        val surfComment = "Surf's up."
        val viewableCase = createViewableCaseWithInterpretation("Case1", 123, listOf(surfComment))
        withClue("sanity check") {
            viewableCase.latestText() shouldBe surfComment
        }

        val deserialized = serializeDeserialize(viewableCase)
        deserialized shouldBe viewableCase
        deserialized.latestText() shouldBe surfComment

        checkSerializationIsThreadSafe(viewableCase)
    }

    private fun caseViewProperties() = CaseViewProperties(listOf(abc, tsh, xyz))

    private fun createCase(name: String): RDRCase {
        val builder = RDRCaseBuilder()
        builder.addValue(tsh, defaultDate, "0.68")
        builder.addValue(xyz, defaultDate, "0.66")
        builder.addValue(abc, defaultDate, "0.67")
        return builder.build(name)
    }
}