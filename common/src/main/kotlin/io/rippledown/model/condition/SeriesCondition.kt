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
data class SeriesCondition(override val id: Int? = null,
                           val attribute: Attribute,
                           val seriesPredicate: SeriesPredicate
): Condition() {
    override fun holds(case: RDRCase): Boolean {
        val values = case.values(attribute) ?: return false
        return seriesPredicate.evaluate(values)
    }

    override fun asText() = seriesPredicate.description(attribute.name)

    override fun alignAttributes(idToAttribute: (Int) -> Attribute) = SeriesCondition(id, idToAttribute(attribute.id), seriesPredicate)

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
    }

    override fun deserialize(decoder: Decoder): SeriesCondition {
        var id: Int? = null
        lateinit var attribute: Attribute
        lateinit var predicate: SeriesPredicate
        decoder.decodeStructure(descriptor) {
            // Loop label needed so that break statement works in js.
            parseLoop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> id = decodeSerializableElement(descriptor, 0, Int.serializer().nullable)
                    1 -> attribute = decodeSerializableElement(descriptor, 1, attributeSerializer)
                    2 -> predicate = decodeSerializableElement(descriptor, 2, predicateSerializer)
                    CompositeDecoder.DECODE_DONE -> break@parseLoop
                    else -> error("Unexpected index: $index")
                }
            }
        }
        return SeriesCondition(id, attribute, predicate)
    }

    override fun serialize(encoder: Encoder, value: SeriesCondition) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, Int.serializer().nullable, value.id)
            encodeSerializableElement(descriptor, 1, attributeSerializer, value.attribute)
            encodeSerializableElement(descriptor,2, predicateSerializer, value.seriesPredicate)
        }
    }
}