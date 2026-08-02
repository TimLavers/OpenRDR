package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.IntRangeData
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.utils.defaultDate
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class CommentTemplateTest {
    private val glucose = Attribute(1, "Glucose")
    private val bmi = Attribute(2, "BMI")

    private fun case(vararg attributeValues: Pair<Attribute, String>): RDRCase {
        val builder = RDRCaseBuilder()
        attributeValues.forEach { (attribute, value) ->
            builder.addValue(attribute, defaultDate, value)
        }
        return builder.build("Case")
    }

    @Test
    fun `a template with no variables evaluates to its text`() {
        // Given a plain template
        val template = CommentTemplate("Diabetic diet advice given.")

        // When it is evaluated
        // Then the text is the value
        template.evaluate(case(glucose to "12.0")) shouldBe "Diabetic diet advice given."
    }

    @Test
    fun `variables are substituted with the latest case values in order`() {
        // Given a template with two variables
        val template = CommentTemplate(
            "Glucose is \${} and BMI is \${}.",
            listOf(glucose, bmi)
        )

        // When it is evaluated against a case with values for both
        val value = template.evaluate(case(glucose to "12.0", bmi to "30.2"))

        // Then the values are substituted in order
        value shouldBe "Glucose is 12.0 and BMI is 30.2."
    }

    @Test
    fun `a variable with no value in the case renders a no-value marker`() {
        // Given a template referencing an attribute missing from the case
        val template = CommentTemplate("BMI is \${}.", listOf(bmi))

        // When it is evaluated
        // Then the marker is rendered in place of the value
        template.evaluate(case(glucose to "12.0")) shouldBe "BMI is {BMI: no value}."
    }

    @Test
    fun `a variable with a blank value renders a no-value marker`() {
        val template = CommentTemplate("BMI is \${}.", listOf(bmi))
        template.evaluate(case(bmi to " ")) shouldBe "BMI is {BMI: no value}."
    }

    @Test
    fun `render reports the ranges of unresolved variables`() {
        // Given a template with one resolvable and one unresolvable variable
        val template = CommentTemplate(
            "Glucose \${}, BMI \${}.",
            listOf(glucose, bmi)
        )

        // When it is rendered against a case with a value for glucose only
        val rendered = template.render(case(glucose to "12.0"))

        // Then the text has the marker and its range is reported
        rendered.text shouldBe "Glucose 12.0, BMI {BMI: no value}."
        val markerStart = rendered.text.indexOf("{BMI: no value}")
        rendered.unresolvedRanges shouldBe
                listOf(IntRangeData(markerStart, markerStart + "{BMI: no value}".length - 1))
    }

    @Test
    fun `render reports no unresolved ranges when all variables resolve`() {
        val template = CommentTemplate("Glucose \${}.", listOf(glucose))
        template.render(case(glucose to "12.0")).unresolvedRanges shouldBe emptyList()
    }

    @Test
    fun `referencedAttributes are the variable attributes`() {
        CommentTemplate("Plain.").referencedAttributes() shouldBe emptySet()
        CommentTemplate("\${} \${}", listOf(glucose, bmi))
            .referencedAttributes() shouldBe setOf(glucose, bmi)
    }

    @Test
    fun `asText shows the variables by attribute name`() {
        // Given a template with variables
        val template = CommentTemplate("Glucose is \${}, BMI is \${}.", listOf(glucose, bmi))

        // Then its text form uses attribute names in braces
        template.asText() shouldBe "\"Glucose is {Glucose}, BMI is {BMI}.\""
    }

    @Test
    fun `asText for a template with no variables is the quoted text`() {
        CommentTemplate("Plain.").asText() shouldBe "\"Plain.\""
    }

    @Test
    fun `alignAttributes re-points the variable attributes`() {
        // Given a template whose variables are stale copies
        val staleGlucose = Attribute(1, "Old name")
        val template = CommentTemplate("Glucose is \${}.", listOf(staleGlucose))

        // When the attributes are aligned by id
        val aligned = template.alignAttributes { id -> if (id == 1) glucose else error("unexpected id $id") }

        // Then the variables are the aligned attributes
        aligned.variables.single().name shouldBe "Glucose"
    }

    @Test
    fun `serialization round trip`() {
        val template = CommentTemplate("Glucose is \${}.", listOf(glucose))
        serializeDeserialize<ValueExpression>(template) shouldBe template
    }
}
