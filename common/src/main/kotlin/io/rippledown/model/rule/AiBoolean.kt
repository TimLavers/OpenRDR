package io.rippledown.model.rule

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * How an [AiBoolean] is evaluated: the provider and model that read the
 * source text, the generation settings, and the versions of the prompt
 * contract and response schema the server used. Stored in the definition
 * so that a result can be explained after the server's defaults move on,
 * and so that changing any of these is an explicit definition revision
 * rather than a side effect of reconfiguring the chat model.
 */
@Serializable
data class AiEvaluationProfile(
    val provider: String,
    val model: String,
    val temperature: Double,
    val promptVersion: Int,
    val schemaVersion: Int
)

/**
 * A labelled example supplied by the expert: the source text and the
 * Boolean it should classify as, optionally with a short explanation.
 */
@Serializable
data class AiBooleanExample(
    val input: String,
    val expected: Boolean,
    val explanation: String? = null
)

/**
 * A Boolean derived attribute whose value is determined by an AI model
 * reading the latest value of one primary attribute against the expert's
 * interpretation criteria and examples. The definition is sufficient to
 * evaluate future cases independently of the conversation that created it.
 *
 * Evaluation needs a network call, so unlike the other expressions it is
 * never evaluated here: the server's evaluator reads a prepared result for
 * this definition and input. Direct evaluation is a programming error, as
 * for [ByDefinition]. See documentation/design/ai_derived_attributes.md.
 */
@Serializable
@SerialName("AiBoolean")
data class AiBoolean(
    val sourceAttribute: Attribute,
    val instructions: String,
    val examples: List<AiBooleanExample> = emptyList(),
    val revision: String,
    val evaluationProfile: AiEvaluationProfile
) : ValueExpression() {
    override fun evaluate(case: RDRCase): String? =
        error("An AiBoolean expression is evaluated through the server's evaluator, not directly.")

    override fun referencedAttributes() = setOf(sourceAttribute)

    override fun asText() = "AI Boolean from ${sourceAttribute.name}: $instructions"

    override fun summary() = "AI derived attribute from ${sourceAttribute.name}"

    override fun alignAttributes(idToAttribute: (Int) -> Attribute) =
        copy(sourceAttribute = idToAttribute(sourceAttribute.id))

    companion object {
        /**
         * The case value a classification materialises as. A valid `false`
         * is a value like any other, distinct from no input and from a
         * failed evaluation, which assign nothing.
         */
        fun caseValue(value: Boolean) = value.toString()
    }
}
