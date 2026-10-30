package io.rippledown.model.caseview

import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import io.rippledown.model.rule.*
import io.rippledown.utils.defaultDate
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class AiCaseViewTest {
    private val source = Attribute(1, "clinical_notes")
    private val target = Attribute(2, "hasDiabetes", AttributeKind.DERIVED)
    private val definition = AiBoolean(source, "Clear personal history", revision = "revision",
        evaluationProfile = AiEvaluationProfile("gemini", "test-model", 0.0, 1, 1))
    private val provenance = AiProvenance(source.name, definition.instructions, "T2DM", 123L, "model-v1", "revision")
    private val json = Json { allowStructuredMapKeys = true }

    private fun view(value: String?): ViewableCase {
        val case = RDRCaseBuilder().apply {
            addValue(source, defaultDate, "T2DM")
            value?.let { addValue(target, defaultDate, it) }
        }.build("Demo")
        case.interpretation.add(RuleSummary(1, assignment = AssignValue(target, definition)))
        return ViewableCase(case, CaseViewProperties(case.attributes.toList()))
    }

    @Test
    fun `false is a value with saved provenance after serialization`() {
        // Given a valid negative classification
        val view = view("false").copy(aiProvenance = mapOf(target.id to provenance.copy(evidence = null)))
        // When the view crosses the API
        val decoded = json.decodeFromString<ViewableCase>(json.encodeToString(view))
        // Then false remains evaluated and absence of evidence is preserved
        decoded shouldBe view
        decoded.derivedValues().single().status shouldBe DerivedValueStatus.Evaluated
        decoded.derivedValues().single().displayValue() shouldBe "false"
        decoded.derivedValues().single().provenance shouldBe provenance.copy(evidence = null)
    }

    @Test
    fun `no input is displayed without turning the diagnosis into a value`() {
        // Given an active AI assignment without a materialised value
        val view = view(null)
        // When the row is mapped
        val row = view.derivedValues().single()
        // Then the diagnostic is separate from the value
        row.value shouldBe ""
        row.status shouldBe DerivedValueStatus.NoInput("clinical_notes")
        row.displayValue() shouldBe "Not evaluated: no clinical_notes value"
    }

    @Test
    fun `older payloads without provenance or incomplete status remain readable`() {
        // Given an old payload containing only the original three fields
        val view = view("true")
        val encoded = json.encodeToString(view)
        // When decoded using the extended serializer
        val decoded = json.decodeFromString<ViewableCase>(encoded)
        // Then the old value and defaults survive
        decoded.aiProvenance shouldBe emptyMap()
        decoded.incomplete shouldBe null
        decoded.derivedValues().single().displayValue() shouldBe "true"
    }

    @Test
    fun `incomplete view carries primary data but no stale derived values or comments`() {
        // Given a diagnostic response carrying primary data
        val primary = view(null).case.copy(interpretation = Interpretation(CaseId(name = "Demo")))
        val failure = IncompleteInterpretation(primary.id, IncompleteCategory.EVALUATION_FAILED,
            target.name, "Unavailable", primary, CaseViewProperties(primary.attributes.toList()))
        // When mapped for display and serialized
        val view = requireNotNull(failure.caseView())
        val decoded = json.decodeFromString<ViewableCase>(json.encodeToString(view))
        // Then no successful interpretation is displayed
        decoded.case.latestValue(source) shouldBe "T2DM"
        decoded.latestText() shouldBe ""
        decoded.incomplete?.reason shouldBe "Unavailable"
        decoded.derivedValues().single().status shouldBe DerivedValueStatus.Failed("Unavailable")
        decoded.derivedValues().single().displayValue() shouldBe "Evaluation failed: Unavailable"
    }
}
