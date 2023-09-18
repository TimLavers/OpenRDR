package io.rippledown.model.condition

import io.rippledown.model.*
import io.rippledown.model.condition.tabular.chain.ChainPredicate
import io.rippledown.model.condition.tabular.predicate.TestResultPredicate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*

// ORD1
@Serializable(TabularConditionSerializer::class)
data class TabularCondition(override val id: Int? = null,
                       val attribute: Attribute,
                       val predicate: TestResultPredicate,
                       val chainPredicate: ChainPredicate): Condition() {

    override fun holds(case: RDRCase): Boolean {
        val values = case.values(attribute) ?: return false
        return chainPredicate.matches(values.map { predicate.evaluate(it) })
    }

    override fun asText() = "${chainPredicate.description()} ${attribute.name} ${predicate.description(chainPredicate.plurality())}".trim()

    override fun alignAttributes(idToAttribute: (Int) -> Attribute) = TabularCondition(id, idToAttribute(attribute.id), predicate, chainPredicate)

    override fun sameAs(other: Condition): Boolean {
        return if (other is TabularCondition) {
            other.attribute.isEquivalent(attribute) && other.predicate == predicate && other.chainPredicate == chainPredicate
        } else false
    }
}

object TabularConditionSerializer: KSerializer<TabularCondition> {
    private val attributeSerializer = Attribute.serializer()
    private val predicateSerializer = TestResultPredicate.serializer()
    private val chainPredicateSerializer = ChainPredicate.serializer()
    override val descriptor = buildClassSerialDescriptor("TabularConditions") {
        element("id", Int.serializer().nullable.descriptor)
        element("attribute", attributeSerializer.descriptor)
        element("predicate", predicateSerializer.descriptor)
        element("chainPredicate", chainPredicateSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): TabularCondition {
        var id: Int? = null
        lateinit var attribute: Attribute
        lateinit var predicate: TestResultPredicate
        lateinit var chainPredicate: ChainPredicate
        decoder.decodeStructure(descriptor) {
            // Loop label needed so that break statement works in js.
            parseLoop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> id = decodeSerializableElement(descriptor, 0, Int.serializer().nullable)
                    1 -> attribute = decodeSerializableElement(descriptor, 1, attributeSerializer)
                    2 -> predicate = decodeSerializableElement(descriptor, 2, predicateSerializer)
                    3 -> chainPredicate = decodeSerializableElement(descriptor, 3, chainPredicateSerializer)
                    CompositeDecoder.DECODE_DONE -> break@parseLoop
                    else -> error("Unexpected index: $index")
                }
            }
        }
        return TabularCondition(id, attribute, predicate, chainPredicate)
    }

    override fun serialize(encoder: Encoder, value: TabularCondition) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, Int.serializer().nullable, value.id)
            encodeSerializableElement(descriptor, 1, attributeSerializer, value.attribute)
            encodeSerializableElement(descriptor,2, predicateSerializer, value.predicate)
            encodeSerializableElement(descriptor,3, chainPredicateSerializer, value.chainPredicate)
        }
    }
}