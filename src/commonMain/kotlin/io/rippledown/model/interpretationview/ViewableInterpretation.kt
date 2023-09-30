package io.rippledown.model.interpretationview

import io.rippledown.model.COMMENT_SEPARATOR
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
    var diffList: DiffList = DiffList()
) {
    fun caseId() = interpretation.caseId

    fun latestText(): String = if (verifiedText != null) verifiedText!! else textGivenByRules()

    //TODO set the ordering from the interpretation view manager
    fun textGivenByRules(): String {
        return interpretation.ruleSummaries.asSequence().map { it.conclusion?.text }
            .filterNotNull()
            .toMutableSet()//eliminate duplicates
            .toMutableList()
            .sortedWith(String.CASE_INSENSITIVE_ORDER).joinToString(COMMENT_SEPARATOR)
    }

    fun numberOfChanges() = diffList.numberOfChanges()
    fun conditionsForConclusion(conclusion: Conclusion) = interpretation.conditionsForConclusion(conclusion)
    fun conclusions() = interpretation.conclusions()
}

object ViewableInterpretationSerializer : KSerializer<ViewableInterpretation> {
    private val interpretationSerializer = Interpretation.serializer()
    private val verifiedTextSerializer = String.serializer().nullable
    private val diffListKSerializer = DiffList.serializer()
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("ViewableInterpretation") {
            element("interp", interpretationSerializer.descriptor)
            element("verify", verifiedTextSerializer.descriptor)
            element("diff", diffListKSerializer.descriptor)
        }

    override fun serialize(encoder: Encoder, value: ViewableInterpretation) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, interpretationSerializer, value.interpretation)
            encodeSerializableElement(descriptor, 1, verifiedTextSerializer, value.verifiedText)
            encodeSerializableElement(descriptor, 2, diffListKSerializer, value.diffList)
        }
    }

    override fun deserialize(decoder: Decoder): ViewableInterpretation {
        var interpretation = Interpretation()
        var verifiedText: String? = null
        var diffList = DiffList()
        decoder.decodeStructure(descriptor) {
            // Loop label needed so that break statement works in js.
            parseLoop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> interpretation = decodeSerializableElement(descriptor, 0, interpretationSerializer)
                    1 -> verifiedText = decodeSerializableElement(descriptor, 1, verifiedTextSerializer)
                    2 -> diffList = decodeSerializableElement(descriptor, 2, diffListKSerializer)
                    CompositeDecoder.DECODE_DONE -> break@parseLoop
                    else -> error("Unexpected index: $index")
                }
            }
        }
        return ViewableInterpretation(interpretation, verifiedText, diffList)
    }
}
