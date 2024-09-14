package io.rippledown.model.condition.edit

import io.rippledown.model.Attribute
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.GreaterThanOrEquals
import io.rippledown.model.condition.episodic.predicate.IsNumeric
import io.rippledown.model.condition.episodic.predicate.TestResultPredicate
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.episodic.signature.Signature
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*

@Serializable
sealed class EditableComparisonCondition(val attribute: Attribute, val initialCutoff: EditableValue, val signature: Signature): EditableCondition {

    override fun fixedTextPart1(): String {
        return "${attribute.name} ${symbol()} "
    }

    override fun editableValue() = initialCutoff

    override fun condition(value: String): Condition {
        require(initialCutoff.type.valid(value))
        val cutoff = initialCutoff.type.convert(value) as Double
        return EpisodicCondition(attribute, predicate(cutoff), Current)
    }

    override fun prerequisite(): Condition {
        return EpisodicCondition(attribute, IsNumeric, signature)
    }

    abstract fun predicate(double: Double): TestResultPredicate
    abstract fun symbol(): String
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EditableComparisonCondition

        if (attribute != other.attribute) return false
        if (initialCutoff != other.initialCutoff) return false
        return signature == other.signature
    }

    override fun hashCode(): Int {
        var result = attribute.hashCode()
        result = 31 * result + initialCutoff.hashCode()
        result = 31 * result + signature.hashCode()
        return result
    }
}

abstract class EditableComparisonConditionSerializer<T: EditableComparisonCondition>: KSerializer<T> {
    abstract fun serialName(): String
    abstract fun build(attribute: Attribute, initialCutoff: EditableValue, signature: Signature): T
    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor(serialName()) {
            element("attribute", Attribute.serializer().descriptor)
            element("initialCutoff", EditableValue.serializer().descriptor)
            element("signature", Signature.serializer().descriptor)
        }

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, Attribute.serializer(), value.attribute)
            encodeSerializableElement(descriptor, 1, EditableValue.serializer(), value.initialCutoff)
            encodeSerializableElement(descriptor, 2, Signature.serializer(), value.signature)
        }
    }

    override fun deserialize(decoder: Decoder): T {
        var attribute: Attribute? = null
        var initialCutoff: EditableValue? = null
        var signature: Signature? = null
        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> attribute = decodeSerializableElement(descriptor, 0, Attribute.serializer())
                    1 -> initialCutoff = decodeSerializableElement(descriptor, 1, EditableValue.serializer())
                    2 -> signature = decodeSerializableElement(descriptor, 2, Signature.serializer())
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
        }
        return build(attribute!!, initialCutoff!!, signature!!)
    }
}

class EditableGreaterThanEqualsConditionSerializer:
    EditableComparisonConditionSerializer<EditableGreaterThanEqualsCondition>() {
    override fun serialName() = "EditableGreaterThanEqualsCondition"
    override fun build(attribute: Attribute, initialCutoff: EditableValue, signature: Signature) = EditableGreaterThanEqualsCondition(attribute, initialCutoff, signature)
}

@Serializable(with = EditableGreaterThanEqualsConditionSerializer::class)
class EditableGreaterThanEqualsCondition(attribute: Attribute, initialCutoff: EditableValue, signature: Signature): EditableComparisonCondition(attribute, initialCutoff, signature) {

    override fun predicate(double: Double) = GreaterThanOrEquals(double)

    override fun symbol() = "≥"
}