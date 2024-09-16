package io.rippledown.model.condition.edit

import io.rippledown.model.*
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.edit.Type.Integer
import io.rippledown.model.condition.episodic.predicate.*
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.episodic.signature.Signature
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*

@Serializable
sealed class EditableExtendedRangeCondition(val attribute: Attribute, val signature: Signature = Current): EditableCondition {
    override fun fixedTextPart1() = condition("1").asText().dropLast(2)
    override fun fixedTextPart2() = "%"
    override fun editableValue() = EditableValue("10", Integer)

    override fun condition(value: String): Condition {
        require(Integer.valid(value))
        val limit = Integer.convert(value) as Int
        return EpisodicCondition(attribute, createRangePredicate(limit), signature)
    }

    override fun prerequisite() = EpisodicCondition(attribute, prerequisitePredicate(), signature)

    abstract fun createRangePredicate(limit: Int): ExtendedRangeFunction
    abstract fun prerequisitePredicate(): TestResultPredicate
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EditableExtendedRangeCondition

        if (attribute != other.attribute) return false
        return signature == other.signature
    }

    override fun hashCode(): Int {
        var result = attribute.hashCode()
        result = 31 * result + signature.hashCode()
        return result
    }
}

abstract class EditableExtendedRangeConditionSerializer<T: EditableExtendedRangeCondition>: KSerializer<T> {
    abstract fun serialName(): String
    abstract fun build(attribute: Attribute, signature: Signature): T
    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor(serialName()) {
            element("attribute", Attribute.serializer().descriptor)
            element("signature", Signature.serializer().descriptor)
        }

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, Attribute.serializer(), value.attribute)
            encodeSerializableElement(descriptor, 1, Signature.serializer(), value.signature)
        }
    }

    override fun deserialize(decoder: Decoder): T {
        var attribute: Attribute? = null
        var signature: Signature? = null
        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> attribute = decodeSerializableElement(descriptor, 0, Attribute.serializer())
                    1 -> signature = decodeSerializableElement(descriptor, 1, Signature.serializer())
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
        }
        return build(attribute!!, signature!!)
    }
}
class EditableExtendedLowRangeConditionSerializer:
    EditableExtendedRangeConditionSerializer<EditableExtendedLowRangeCondition>() {
    override fun serialName() = "EditableExtendedLowRangeCondition"
    override fun build(attribute: Attribute, signature: Signature) = EditableExtendedLowRangeCondition(attribute, signature)
}
@Serializable(with = EditableExtendedLowRangeConditionSerializer::class)
class EditableExtendedLowRangeCondition(attribute: Attribute, signature: Signature): EditableExtendedRangeCondition(attribute, signature) {
    override fun createRangePredicate(limit: Int) = LowByAtMostSomePercentage(limit)
    override fun prerequisitePredicate() = Low
}
class EditableExtendedLowNormalRangeConditionSerializer:
    EditableExtendedRangeConditionSerializer<EditableExtendedLowNormalRangeCondition>() {
    override fun serialName() = "EditableExtendedLowNormalRangeCondition"
    override fun build(attribute: Attribute, signature: Signature) = EditableExtendedLowNormalRangeCondition(attribute, signature)
}
@Serializable(with = EditableExtendedLowNormalRangeConditionSerializer::class)
class EditableExtendedLowNormalRangeCondition(attribute: Attribute, signature: Signature): EditableExtendedRangeCondition(attribute, signature) {
    override fun createRangePredicate(limit: Int) = NormalOrLowByAtMostSomePercentage(limit)
    override fun prerequisitePredicate() = LowOrNormal
}
class EditableExtendedHighNormalRangeConditionSerializer:
    EditableExtendedRangeConditionSerializer<EditableExtendedHighNormalRangeCondition>() {
    override fun serialName() = "EditableExtendedHighNormalRangeCondition"
    override fun build(attribute: Attribute, signature: Signature) = EditableExtendedHighNormalRangeCondition(attribute, signature)
}
@Serializable(with = EditableExtendedHighNormalRangeConditionSerializer::class)
class EditableExtendedHighNormalRangeCondition(attribute: Attribute, signature: Signature): EditableExtendedRangeCondition(attribute, signature) {
    override fun createRangePredicate(limit: Int) = NormalOrHighByAtMostSomePercentage(limit)
    override fun prerequisitePredicate() = HighOrNormal
}
class EditableExtendedHighRangeConditionSerializer:
    EditableExtendedRangeConditionSerializer<EditableExtendedHighRangeCondition>() {
    override fun serialName() = "EditableExtendedHighRangeCondition"
    override fun build(attribute: Attribute, signature: Signature) = EditableExtendedHighRangeCondition(attribute, signature)
}
@Serializable(with = EditableExtendedHighRangeConditionSerializer::class)
class EditableExtendedHighRangeCondition(attribute: Attribute, signature: Signature): EditableExtendedRangeCondition(attribute, signature) {
    override fun createRangePredicate(limit: Int) = HighByAtMostSomePercentage(limit)
    override fun prerequisitePredicate() = High
}