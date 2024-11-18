package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.series.SeriesPredicate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*

// ORD1
/**
 * A condition that takes the entire sequence of values for an attribute
 * and evaluates that using a series predicate.
 */
@Serializable(SeriesConditionSerializer::class)
data class SeriesCondition(
    override val id: Int? = null,
    val attribute: Attribute,
    val seriesPredicate: SeriesPredicate,
    val userExpression: String = ""
): Condition() {
    override fun holds(case: RDRCase): Boolean {
        val values = case.values(attribute) ?: return false
        return seriesPredicate.evaluate(values)
    }

    override fun asText() = seriesPredicate.description(attribute.name)

    override fun userExpression() = userExpression

    override fun alignAttributes(idToAttribute: (Int) -> Attribute) = SeriesCondition(
        id,
        idToAttribute(attribute.id),
        seriesPredicate,
        userExpression
    )

    override fun sameAs(other: Condition): Boolean {
        return if (other is SeriesCondition) {
            other.attribute.isEquivalent(attribute) && other.seriesPredicate == seriesPredicate
        } else false
    }

    override fun attributeNames() = setOf(attribute.name)
}
object SeriesConditionSerializer: KSerializer<SeriesCondition> {
    private val attributeSerializer = Attribute.serializer()
    private val predicateSerializer = SeriesPredicate.serializer()
    override val descriptor = buildClassSerialDescriptor("SeriesCondition") {
        element("id", Int.serializer().nullable.descriptor)
        element("attribute", attributeSerializer.descriptor)
        element("seriesPredicate", predicateSerializer.descriptor)
        element("userExpression", String.serializer().descriptor)
    }

    override fun deserialize(decoder: Decoder): SeriesCondition {
        var id: Int? = null
        lateinit var attribute: Attribute
        lateinit var predicate: SeriesPredicate
        lateinit var userExpression: String
        decoder.decodeStructure(descriptor) {
            // Loop label needed so that break statement works in js.
            parseLoop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> id = decodeSerializableElement(descriptor, 0, Int.serializer().nullable)
                    1 -> attribute = decodeSerializableElement(descriptor, 1, attributeSerializer)
                    2 -> predicate = decodeSerializableElement(descriptor, 2, predicateSerializer)
                    3 -> userExpression = decodeSerializableElement(descriptor, 3, String.serializer())
                    CompositeDecoder.DECODE_DONE -> break@parseLoop
                    else -> error("Unexpected index: $index")
                }
            }
        }
        return SeriesCondition(id, attribute, predicate, userExpression)
    }

    override fun serialize(encoder: Encoder, value: SeriesCondition) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, Int.serializer().nullable, value.id)
            encodeSerializableElement(descriptor, 1, attributeSerializer, value.attribute)
            encodeSerializableElement(descriptor,2, predicateSerializer, value.seriesPredicate)
            encodeSerializableElement(descriptor, 3, String.serializer(), value.userExpression)
        }
    }
}