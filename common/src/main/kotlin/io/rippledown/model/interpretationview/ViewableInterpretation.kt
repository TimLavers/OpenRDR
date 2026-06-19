package io.rippledown.model.interpretationview

import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.RenderedComment
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*

@Serializable(ViewableInterpretationSerializer::class)
data class ViewableInterpretation(
    val interpretation: Interpretation = Interpretation(),
    var textGivenByRules: String = interpretation.conclusionTexts().joinToString(" "),
    var renderedComments: List<RenderedComment> = emptyList()
) {
    fun caseId() = interpretation.caseId
    fun latestText() = textGivenByRules
    fun conditionsForConclusion(conclusion: Conclusion) = interpretation.conditionsForConclusion(conclusion)
    fun conclusions() = interpretation.conclusions()
}

object ViewableInterpretationSerializer : KSerializer<ViewableInterpretation> {
    private val interpretationSerializer = Interpretation.serializer()
    private val textGivenByRulesSerializer = String.serializer()
    private val renderedCommentsSerializer = ListSerializer(RenderedComment.serializer())
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("ViewableInterpretation") {
            element("interp", interpretationSerializer.descriptor)
            element("text", textGivenByRulesSerializer.descriptor)
            element("renderedComments", renderedCommentsSerializer.descriptor)
        }

    override fun serialize(encoder: Encoder, value: ViewableInterpretation) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, interpretationSerializer, value.interpretation)
            encodeSerializableElement(descriptor, 1, textGivenByRulesSerializer, value.textGivenByRules)
            encodeSerializableElement(descriptor, 2, renderedCommentsSerializer, value.renderedComments)
        }
    }

    override fun deserialize(decoder: Decoder): ViewableInterpretation {
        var interpretation = Interpretation()
        var textGivenByRules = ""
        var renderedComments = emptyList<RenderedComment>()
        decoder.decodeStructure(descriptor) {
            // Loop label needed so that break statement works in js.
            parseLoop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> interpretation = decodeSerializableElement(descriptor, 0, interpretationSerializer)
                    1 -> textGivenByRules = decodeSerializableElement(descriptor, 1, textGivenByRulesSerializer)
                    2 -> renderedComments = decodeSerializableElement(descriptor, 2, renderedCommentsSerializer)
                    CompositeDecoder.DECODE_DONE -> break@parseLoop
                    else -> error("Unexpected index: $index")
                }
            }
        }
        return ViewableInterpretation(interpretation, textGivenByRules, renderedComments)
    }
}
