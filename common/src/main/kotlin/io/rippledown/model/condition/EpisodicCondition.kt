package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.episodic.predicate.TestResultPredicate
import io.rippledown.model.condition.episodic.signature.Signature
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*

// ORD1
/**
 * A condition that evaluates the test results for an attribute
 * against a predicate, episode by episode, to produce a pattern
 * of booleans that is then evaluated using a signature function.
 */
@Serializable(EpisodicConditionSerializer::class)
data class EpisodicCondition(
    override val id: Int? = null,
    val attribute: Attribute,
    val predicate: TestResultPredicate,
    val signature: Signature,
    val userExpression: String = ""
) : Condition() {

    constructor(attribute: Attribute, predicate: TestResultPredicate, signature: Signature) : this(
        null,
        attribute,
        predicate,
        signature,
        ""
    )

    override fun userExpression() = userExpression

    override fun holds(case: RDRCase): Boolean {
        val values = case.values(attribute) ?: return false
        return signature.matches(values.map { predicate.evaluate(it) })
    }

    override fun asText() =
        "${signature.description()} ${attribute.name} ${predicate.description(signature.plurality())}".trim()

    override fun alignAttributes(idToAttribute: (Int) -> Attribute) = EpisodicCondition(
        id,
        idToAttribute(attribute.id),
        predicate,
        signature,
        userExpression
    )

    override fun sameAs(other: Condition): Boolean {
        return if (other is EpisodicCondition) {
            other.attribute.isEquivalent(attribute) && other.predicate == predicate && other.signature == signature
        } else false
    }

    override fun attributeNames() = setOf(attribute.name)
}

object EpisodicConditionSerializer : KSerializer<EpisodicCondition> {
    private val attributeSerializer = Attribute.serializer()
    private val predicateSerializer = TestResultPredicate.serializer()
    private val signatureSerializer = Signature.serializer()
    override val descriptor = buildClassSerialDescriptor("TabularConditions") {
        element("id", Int.serializer().nullable.descriptor)
        element("attribute", attributeSerializer.descriptor)
        element("predicate", predicateSerializer.descriptor)
        element("chainPredicate", signatureSerializer.descriptor)
        element("userExpression", String.serializer().descriptor)
    }

    override fun deserialize(decoder: Decoder): EpisodicCondition {
        var id: Int? = null
        lateinit var attribute: Attribute
        lateinit var predicate: TestResultPredicate
        lateinit var signature: Signature
        lateinit var userExpression: String
        decoder.decodeStructure(descriptor) {
            // Loop label needed so that break statement works in js.
            parseLoop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> id = decodeSerializableElement(descriptor, 0, Int.serializer().nullable)
                    1 -> attribute = decodeSerializableElement(descriptor, 1, attributeSerializer)
                    2 -> predicate = decodeSerializableElement(descriptor, 2, predicateSerializer)
                    3 -> signature = decodeSerializableElement(descriptor, 3, signatureSerializer)
                    4 -> userExpression = decodeSerializableElement(descriptor, 4, String.serializer())
                    CompositeDecoder.DECODE_DONE -> break@parseLoop
                    else -> error("Unexpected index: $index")
                }
            }
        }
        return EpisodicCondition(id, attribute, predicate, signature, userExpression)
    }

    override fun serialize(encoder: Encoder, value: EpisodicCondition) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, Int.serializer().nullable, value.id)
            encodeSerializableElement(descriptor, 1, attributeSerializer, value.attribute)
            encodeSerializableElement(descriptor, 2, predicateSerializer, value.predicate)
            encodeSerializableElement(descriptor, 3, signatureSerializer, value.signature)
            encodeSerializableElement(descriptor, 4, String.serializer(), value.userExpression)
        }
    }
}