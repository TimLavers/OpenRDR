package io.rippledown.model.interpretationview

import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.diff.DiffList
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*

@Serializable(ViewableInterpretationSerializer::class)
data class ViewableInterpretation(
    val interpretation: Interpretation = Interpretation(),
    var verifiedText: String? = null,
    var diffList: DiffList = DiffList(),
    var textGivenByRules: String = interpretation.conclusionTexts().joinToString(" ")
) {
    fun caseId() = interpretation.caseId
    fun latestText(): String = if (verifiedText != null) verifiedText!! else textGivenByRules
    fun numberOfChanges() = diffList.numberOfChanges()
    fun conditionsForConclusion(conclusion: Conclusion) = interpretation.conditionsForConclusion(conclusion)
    fun conclusions() = interpretation.conclusions()
}

object ViewableInterpretationSerializer : KSerializer<ViewableInterpretation> {
    private val interpretationSerializer = Interpretation.serializer()
    private val verifiedTextSerializer = String.serializer().nullable
    private val diffListSerializer = DiffList.serializer()
    private val textGivenByRulesSerializer = String.serializer()
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("ViewableInterpretation") {
            element("interp", interpretationSerializer.descriptor)
            element("verify", verifiedTextSerializer.descriptor)
            element("diff", diffListSerializer.descriptor)
            element("text", textGivenByRulesSerializer.descriptor)
        }

    override fun serialize(encoder: Encoder, value: ViewableInterpretation) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, interpretationSerializer, value.interpretation)
            encodeSerializableElement(descriptor, 1, verifiedTextSerializer, value.verifiedText)
            encodeSerializableElement(descriptor, 2, diffListSerializer, value.diffList)
            encodeSerializableElement(descriptor, 3, textGivenByRulesSerializer, value.textGivenByRules)
        }
    }

    override fun deserialize(decoder: Decoder): ViewableInterpretation {
        var interpretation = Interpretation()
        var verifiedText: String? = null
        var diffList = DiffList()
        var textGivenByRules = ""
        decoder.decodeStructure(descriptor) {
            // Loop label needed so that break statement works in js.
            parseLoop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> interpretation = decodeSerializableElement(descriptor, 0, interpretationSerializer)
                    1 -> verifiedText = decodeSerializableElement(descriptor, 1, verifiedTextSerializer)
                    2 -> diffList = decodeSerializableElement(descriptor, 2, diffListSerializer)
                    3 -> textGivenByRules = decodeSerializableElement(descriptor, 3, textGivenByRulesSerializer)
                    CompositeDecoder.DECODE_DONE -> break@parseLoop
                    else -> error("Unexpected index: $index")
                }
            }
        }
        return ViewableInterpretation(interpretation, verifiedText, diffList, textGivenByRules)
    }
}
