package io.rippledown.hints

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@Serializable(with = FunctionSpecificationSerializer::class)
data class FunctionSpecification(
    val name: String = "",
    val parameters: List<String> = listOf()
)

object FunctionSpecificationSerializer : KSerializer<FunctionSpecification> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("FunctionSpecification") {
        element("name", String.serializer().descriptor)
        element("parameters", ListSerializer(JsonPrimitive.serializer()).descriptor, isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: FunctionSpecification) {
        with(encoder.beginStructure(descriptor)) {
            encodeStringElement(descriptor, 0, value.name)
            encodeSerializableElement(
                descriptor,
                1,
                ListSerializer(JsonPrimitive.serializer()),
                value.parameters.map { JsonPrimitive(it) })
            endStructure(descriptor)
        }
    }

    override fun deserialize(decoder: Decoder): FunctionSpecification {
        with(decoder.beginStructure(descriptor)) {
            var name = ""
            var parameters = listOf<String>()
            loop@ while (true) {
                when (decodeElementIndex(descriptor)) {
                    0 -> name = decodeStringElement(descriptor, 0)
                    1 -> {
                        val jsonElement = decodeSerializableElement(descriptor, 1, JsonElement.serializer())
                        parameters = when (jsonElement) {
                            is JsonArray -> jsonElement.jsonArray.map { it.jsonPrimitive.content }
                            is JsonNull -> emptyList()
                            is JsonPrimitive -> if (jsonElement.content.isEmpty()) emptyList() else listOf(jsonElement.content)
                            else -> emptyList()
                        }
                    }

                    else -> break@loop
                }
            }
            endStructure(descriptor)
            return FunctionSpecification(name, parameters)
        }
    }
}