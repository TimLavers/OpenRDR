package io.rippledown.model.rule.evaluation

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.caseview.AiProvenance
import io.rippledown.model.rule.*
import io.rippledown.utils.defaultDate
import kotlin.test.Test

internal class DefaultValueExpressionEvaluatorTest {
    private val notes = Attribute(1, "clinical_notes")
    private val weight = Attribute(2, "weight")
    private val bmi = Attribute(10, "BMI", AttributeKind.DERIVED)
    private val profile = AiEvaluationProfile("gemini", "gemini-2.5-flash", 0.0, 1, 1)
    private val hasDiabetes = AiBoolean(
        sourceAttribute = notes,
        instructions = "True if the notes affirm a personal history of diabetes.",
        revision = "rev-1",
        evaluationProfile = profile
    )
    private val evaluator = DefaultValueExpressionEvaluator()

    private fun case(vararg attributeToValue: Pair<Attribute, String>) = with(RDRCaseBuilder()) {
        attributeToValue.forEach { addValue(it.first, defaultDate, it.second) }
        build("Case")
    }

    private fun emptyCase(): RDRCase = RDRCaseBuilder().build("Empty")

    @Test
    fun `a literal evaluates locally to a value`() {
        evaluator.evaluate(Literal("diabetic"), case(weight to "90"), EvaluationContext.EMPTY) shouldBe
                EvaluationOutcome.Value("diabetic")
    }

    @Test
    fun `a formula evaluates locally to a value`() {
        val formula = Formula(Binary(Operator.PLUS, AttributeValue(weight), Num(1.0)))
        evaluator.evaluate(formula, case(weight to "90"), EvaluationContext.EMPTY) shouldBe
                EvaluationOutcome.Value("91")
    }

    @Test
    fun `a formula with a missing input has no input`() {
        val formula = Formula(Binary(Operator.PLUS, AttributeValue(weight), Num(1.0)))
        evaluator.evaluate(formula, case(notes to "x"), EvaluationContext.EMPTY) shouldBe EvaluationOutcome.NoInput
    }

    @Test
    fun `a comment template evaluates locally`() {
        evaluator.evaluate(CommentTemplate("Go to the beach."), case(weight to "90"), EvaluationContext.EMPTY) shouldBe
                EvaluationOutcome.Value("Go to the beach.")
    }

    @Test
    fun `a by-definition expression must have been resolved first`() {
        shouldThrow<IllegalStateException> {
            evaluator.evaluate(ByDefinition, case(weight to "90"), EvaluationContext.EMPTY)
        }
    }

    @Test
    fun `an AI Boolean with no source value in the latest episode has no input`() {
        evaluator.evaluate(hasDiabetes, case(weight to "90"), EvaluationContext.EMPTY) shouldBe
                EvaluationOutcome.NoInput
    }

    @Test
    fun `an AI Boolean with a blank source value has no input`() {
        evaluator.evaluate(hasDiabetes, case(notes to "   "), EvaluationContext.EMPTY) shouldBe
                EvaluationOutcome.NoInput
    }

    @Test
    fun `an AI Boolean on a case with no episodes has no input`() {
        evaluator.evaluate(hasDiabetes, emptyCase(), EvaluationContext.EMPTY) shouldBe EvaluationOutcome.NoInput
    }

    @Test
    fun `an AI Boolean reads the latest episode only`() {
        // Given notes in an older episode but none in the latest
        val case = with(RDRCaseBuilder()) {
            addValue(notes, defaultDate - 1000, "T2 Diab")
            addValue(weight, defaultDate, "90")
            build("Case")
        }

        // When evaluated
        // Then there is no input: an older value is not searched for
        evaluator.evaluate(hasDiabetes, case, EvaluationContext.EMPTY) shouldBe EvaluationOutcome.NoInput
    }

    @Test
    fun `an AI Boolean with nonblank input and no prepared result needs evaluation`() {
        // When evaluated against a case with notes but an empty context
        val outcome = evaluator.evaluate(hasDiabetes, case(notes to "T2 Diab"), EvaluationContext.EMPTY)

        // Then the demand names the exact definition and input text
        outcome shouldBe EvaluationOutcome.NeedsEvaluation(AiEvaluationRequest(hasDiabetes, "T2 Diab"))
    }

    @Test
    fun `the request preserves the input bytes exactly`() {
        // Given notes with surrounding whitespace
        val outcome = evaluator.evaluate(hasDiabetes, case(notes to "  T2 Diab \n"), EvaluationContext.EMPTY)

        // Then the input is not normalised
        outcome.shouldBeInstanceOf<EvaluationOutcome.NeedsEvaluation>().request.input shouldBe "  T2 Diab \n"
    }

    @Test
    fun `a prepared true becomes the canonical value with provenance`() {
        // Given a context holding a successful result for this definition and input
        val request = AiEvaluationRequest(hasDiabetes, "T2 Diab")
        val result = AiEvaluationResult.Success(
            value = true,
            evidence = "T2 Diab",
            evaluatedAt = 1_700_000_000_000,
            model = "gemini-2.5-flash-001"
        )
        val context = EvaluationContext.EMPTY.with(request, result)

        // When evaluated
        val outcome = evaluator.evaluate(hasDiabetes, case(notes to "T2 Diab"), context)

        // Then the value is "true" and the provenance describes how it was obtained
        outcome shouldBe EvaluationOutcome.Value(
            "true",
            AiProvenance(
                sourceName = "clinical_notes",
                instructions = hasDiabetes.instructions,
                evidence = "T2 Diab",
                evaluatedAt = 1_700_000_000_000,
                model = "gemini-2.5-flash-001",
                revision = "rev-1"
            )
        )
    }

    @Test
    fun `a prepared false is a value, not an absence`() {
        val request = AiEvaluationRequest(hasDiabetes, "FH diab")
        val context = EvaluationContext.EMPTY.with(request, AiEvaluationResult.Success(false, null, 1L, "m"))

        val outcome = evaluator.evaluate(hasDiabetes, case(notes to "FH diab"), context)

        outcome.shouldBeInstanceOf<EvaluationOutcome.Value>().value shouldBe "false"
    }

    @Test
    fun `a prepared failure is reported as failed, not as no input`() {
        val request = AiEvaluationRequest(hasDiabetes, "T2 Diab")
        val context = EvaluationContext.EMPTY.with(request, AiEvaluationResult.Failure("Gemini unavailable"))

        evaluator.evaluate(hasDiabetes, case(notes to "T2 Diab"), context) shouldBe
                EvaluationOutcome.Failed("Gemini unavailable")
    }

    @Test
    fun `a result for a different input does not satisfy the request`() {
        val context = EvaluationContext.EMPTY
            .with(AiEvaluationRequest(hasDiabetes, "FH diab"), AiEvaluationResult.Success(false, null, 1L, "m"))

        evaluator.evaluate(hasDiabetes, case(notes to "T2 Diab"), context) shouldBe
                EvaluationOutcome.NeedsEvaluation(AiEvaluationRequest(hasDiabetes, "T2 Diab"))
    }

    @Test
    fun `a result for a different revision does not satisfy the request`() {
        // Given a result obtained under an earlier revision of the definition
        val earlier = hasDiabetes.copy(revision = "rev-0")
        val context = EvaluationContext.EMPTY
            .with(AiEvaluationRequest(earlier, "T2 Diab"), AiEvaluationResult.Success(true, null, 1L, "m"))

        // When the current revision is evaluated on the same input
        // Then a new evaluation is needed
        evaluator.evaluate(hasDiabetes, case(notes to "T2 Diab"), context) shouldBe
                EvaluationOutcome.NeedsEvaluation(AiEvaluationRequest(hasDiabetes, "T2 Diab"))
    }

    @Test
    fun `a renamed source still matches its prepared result`() {
        // Given a result prepared when the source attribute had its old name
        val context = EvaluationContext.EMPTY
            .with(AiEvaluationRequest(hasDiabetes, "T2 Diab"), AiEvaluationResult.Success(true, null, 1L, "m"))

        // When the definition is aligned to the renamed source (same id)
        val renamed = hasDiabetes.alignAttributes { Attribute(1, "Notes") }
        val case = with(RDRCaseBuilder()) {
            addValue(Attribute(1, "Notes"), defaultDate, "T2 Diab")
            build("Case")
        }

        // Then the result is found: identity is by attribute id, not name
        evaluator.evaluate(renamed, case, context).shouldBeInstanceOf<EvaluationOutcome.Value>().value shouldBe "true"
    }

    @Test
    fun `adding to a context does not change the original`() {
        // Given an empty context
        val request = AiEvaluationRequest(hasDiabetes, "T2 Diab")

        // When a result is added
        val extended = EvaluationContext.EMPTY.with(request, AiEvaluationResult.Success(true, null, 1L, "m"))

        // Then the original is still empty
        EvaluationContext.EMPTY.resultFor(request) shouldBe null
        extended.resultFor(request) shouldBe AiEvaluationResult.Success(true, null, 1L, "m")
    }

    @Test
    fun `the evaluator ignores results for other attributes`() {
        // Given a context with a result for an AI Boolean, and a formula to evaluate
        val context = EvaluationContext.EMPTY
            .with(AiEvaluationRequest(hasDiabetes, "x"), AiEvaluationResult.Success(true, null, 1L, "m"))
        val formula = Formula(AttributeValue(bmi))

        // When the formula is evaluated on a case lacking BMI
        // Then the context is irrelevant to it
        evaluator.evaluate(formula, case(notes to "x"), context) shouldBe EvaluationOutcome.NoInput
    }
}
