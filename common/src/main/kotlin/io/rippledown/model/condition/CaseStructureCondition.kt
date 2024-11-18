package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.structural.CaseStructurePredicate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*

// ORD1
/**
 * A condition that evaluates the overall structure of a case, looking at
 * properties such as the presence or absence of an attribute, or the total
 * number of episodes.
 */
@Serializable(CaseStructureConditionSerializer::class)
data class CaseStructureCondition(
    override val id: Int? = null,
    val predicate: CaseStructurePredicate,
    val userExpression: String = ""
) : Condition() {

    constructor(predicate: CaseStructurePredicate) : this(null, predicate)

    override fun holds(case: RDRCase) = predicate.evaluate(case)

    override fun asText() = predicate.description()

    override fun userExpression() = userExpression

    override fun alignAttributes(idToAttribute: (Int) -> Attribute) = CaseStructureCondition(
        id,
        predicate.alignAttributes(idToAttribute),
        ""
    )

    override fun sameAs(other: Condition): Boolean {
        return if (other is CaseStructureCondition) {
            other.predicate == predicate
        } else false
    }

    override fun attributeNames() = predicate.attributeNames()
}

object CaseStructureConditionSerializer: KSerializer<CaseStructureCondition> {
    private val predicateSerializer = CaseStructurePredicate.serializer()
    override val descriptor = buildClassSerialDescriptor("CaseStructureCondition") {
        element("id", Int.serializer().nullable.descriptor)
        element("structurePredicate", predicateSerializer.descriptor)
        element("userExpression", String.serializer().descriptor)
    }

    override fun deserialize(decoder: Decoder): CaseStructureCondition {
        var id: Int? = null
        lateinit var predicate: CaseStructurePredicate
        lateinit var userExpression: String
        decoder.decodeStructure(descriptor) {
            // Loop label needed so that break statement works in js.
            parseLoop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> id = decodeSerializableElement(descriptor, 0, Int.serializer().nullable)
                    1 -> predicate = decodeSerializableElement(descriptor, 1, predicateSerializer)
                    2 -> userExpression = decodeSerializableElement(descriptor, 2, String.serializer())
                    CompositeDecoder.DECODE_DONE -> break@parseLoop
                    else -> error("Unexpected index: $index")
                }
            }
        }
        return CaseStructureCondition(id, predicate, userExpression)
    }

    override fun serialize(encoder: Encoder, value: CaseStructureCondition) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, Int.serializer().nullable, value.id)
            encodeSerializableElement(descriptor,1, predicateSerializer, value.predicate)
            encodeSerializableElement(descriptor, 2, String.serializer().nullable, value.userExpression)
        }
    }
}
