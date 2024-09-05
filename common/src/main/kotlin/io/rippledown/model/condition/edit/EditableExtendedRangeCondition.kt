package io.rippledown.model.condition.edit

import io.rippledown.model.*
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.edit.Type.Integer
import io.rippledown.model.condition.episodic.predicate.*
import io.rippledown.model.condition.episodic.signature.Current
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*

@Serializable
sealed class EditableExtendedRangeCondition(val attribute: Attribute): EditableCondition {
    override fun fixedTextPart1() = condition("1").asText().dropLast(2)
    override fun fixedTextPart2() = "%"
    override fun editableValue() = EditableValue("10", Integer)

    override fun condition(value: String): Condition {
        require(Integer.valid(value))
        val limit = Integer.convert(value) as Int
        return EpisodicCondition(attribute, createRangePredicate(limit), Current)
    }

    abstract fun createRangePredicate(limit: Int): ExtendedRangeFunction
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EditableExtendedRangeCondition
        return attribute == other.attribute
    }

    override fun hashCode(): Int {
        return attribute.hashCode()
    }
}

abstract class EditableExtendedRangeConditionSerializer<T: EditableExtendedRangeCondition>: KSerializer<T> {
    abstract fun serialName(): String
    abstract fun build(attribute: Attribute): T
    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor(serialName()) {
            element("attribute", Attribute.serializer().descriptor)
        }

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, Attribute.serializer(), value.attribute)
        }
    }

    override fun deserialize(decoder: Decoder): T {
        var attribute: Attribute? = null
        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> attribute = decodeSerializableElement(descriptor, 0, Attribute.serializer())
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
        }
        return build(attribute!!)
    }
}
class EditableExtendedLowRangeConditionSerializer:
    EditableExtendedRangeConditionSerializer<EditableExtendedLowRangeCondition>() {
    override fun serialName() = "EditableExtendedLowRangeCondition"
    override fun build(attribute: Attribute) = EditableExtendedLowRangeCondition(attribute)
}
@Serializable(with = EditableExtendedLowRangeConditionSerializer::class)
class EditableExtendedLowRangeCondition(attribute: Attribute): EditableExtendedRangeCondition(attribute) {
    override fun createRangePredicate(limit: Int) = LowByAtMostSomePercentage(limit)
}
class EditableExtendedLowNormalRangeConditionSerializer:
    EditableExtendedRangeConditionSerializer<EditableExtendedLowNormalRangeCondition>() {
    override fun serialName() = "EditableExtendedLowNormalRangeCondition"
    override fun build(attribute: Attribute) = EditableExtendedLowNormalRangeCondition(attribute)
}
@Serializable(with = EditableExtendedLowNormalRangeConditionSerializer::class)
class EditableExtendedLowNormalRangeCondition(attribute: Attribute): EditableExtendedRangeCondition(attribute) {
    override fun createRangePredicate(limit: Int) = NormalOrLowByAtMostSomePercentage(limit)
}
class EditableExtendedHighNormalRangeConditionSerializer:
    EditableExtendedRangeConditionSerializer<EditableExtendedHighNormalRangeCondition>() {
    override fun serialName() = "EditableExtendedHighNormalRangeCondition"
    override fun build(attribute: Attribute) = EditableExtendedHighNormalRangeCondition(attribute)
}
@Serializable(with = EditableExtendedHighNormalRangeConditionSerializer::class)
class EditableExtendedHighNormalRangeCondition(attribute: Attribute): EditableExtendedRangeCondition(attribute) {
    override fun createRangePredicate(limit: Int) = NormalOrHighByAtMostSomePercentage(limit)
}
class EditableExtendedHighRangeConditionSerializer:
    EditableExtendedRangeConditionSerializer<EditableExtendedHighRangeCondition>() {
    override fun serialName() = "EditableExtendedHighRangeCondition"
    override fun build(attribute: Attribute) = EditableExtendedHighRangeCondition(attribute)
}
@Serializable(with = EditableExtendedHighRangeConditionSerializer::class)
class EditableExtendedHighRangeCondition(attribute: Attribute): EditableExtendedRangeCondition(attribute) {
    override fun createRangePredicate(limit: Int) = HighByAtMostSomePercentage(limit)
}