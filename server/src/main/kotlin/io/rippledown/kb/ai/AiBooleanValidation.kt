package io.rippledown.kb.ai

import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.rule.AiBoolean

/**
 * Checks that an AI Boolean definition can be stored in a knowledge base
 * holding [knownAttributes]. Deserialization accepts any well-formed
 * definition; this is the semantic check, run before a draft mutates the
 * KB. Throws [IllegalArgumentException] with a message fit for the chat.
 */
fun validateAiBoolean(definition: AiBoolean, knownAttributes: Set<Attribute>) {
    require(definition.revision.isNotBlank()) { "An AI Boolean definition must carry a revision." }
    val source = knownAttributes.firstOrNull { it.id == definition.sourceAttribute.id }
    requireNotNull(source) {
        "The source attribute \"${definition.sourceAttribute.name}\" is not in this knowledge base."
    }
    require(source.kind == AttributeKind.EXTERNAL) {
        "The source of an AI Boolean must be a primary attribute, but \"${source.name}\" is assigned by the knowledge base."
    }
    require(definition.instructions.isNotBlank()) { "The interpretation criteria cannot be blank." }
    definition.examples.forEach { require(it.input.isNotBlank()) { "An example cannot have blank text." } }
    definition.examples.groupBy { it.input }.forEach { (input, examples) ->
        require(examples.map { it.expected }.distinct().size == 1) {
            "The example \"$input\" is labelled both true and false."
        }
    }
    require(AiEvaluationPolicy.supports(definition.evaluationProfile)) {
        with(definition.evaluationProfile) {
            "The evaluation profile (provider \"$provider\", prompt version $promptVersion, schema version $schemaVersion) is not supported by this server."
        }
    }
}

/**
 * Checks that [name] can be given to a new attribute, under the same rules
 * AttributeManager applies when creating one.
 */
fun validateNewAiAttributeName(name: String, knownAttributes: Set<Attribute>) {
    require(name.isNotBlank()) { "An attribute name cannot be blank." }
    val conflicting = knownAttributes.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
    if (conflicting != null) {
        throw IllegalArgumentException("An attribute with name \"${conflicting.name}\" already exists. Choose a different name.")
    }
}
