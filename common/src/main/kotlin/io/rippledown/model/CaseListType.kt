package io.rippledown.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(CaseListTypeSerializer::class)
class CaseListType(val name: String) {

    private val key = name.lowercase()

    val isBuiltIn get() = this == Processed || this == Cornerstone

    override fun equals(other: Any?) = other is CaseListType && other.key == key

    override fun hashCode() = key.hashCode()

    override fun toString() = name

    companion object {
        val Processed = CaseListType("Processed")
        val Cornerstone = CaseListType("Cornerstone")

        private val reservedNames = setOf("processed", "cornerstone", "cornerstones")

        fun isReservedListName(name: String): Boolean {
            val normalised = name.trim().lowercase()
                .removeSuffix(" case list")
                .removeSuffix(" cases")
                .trim()
            return normalised in reservedNames
        }
    }
}

object CaseListTypeSerializer : KSerializer<CaseListType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("CaseListType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: CaseListType) = encoder.encodeString(value.name)

    override fun deserialize(decoder: Decoder) = CaseListType(decoder.decodeString())
}
