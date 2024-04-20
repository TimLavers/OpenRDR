package io.rippledown.model.caseview

import io.rippledown.model.CaseId
import io.rippledown.model.RDRCase
import io.rippledown.model.interpretationview.ViewableInterpretation
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*

@Serializable(ViewableCaseSerializer::class)
data class ViewableCase(
    val case: RDRCase,
    val viewProperties: CaseViewProperties = CaseViewProperties(),
    var viewableInterpretation: ViewableInterpretation = ViewableInterpretation(case.interpretation)
) {
    val name = case.caseId.name
    val id = case.caseId.id
    val dates = case.dates
    val numberOfColumns = dates.size

    init {
        check(case.attributes == viewProperties.attributes.toSet()) {
            "Case attributes do not match view properties attributes:\n\nCase attributes: ${case.attributes}\n\nView properties attributes: ${viewProperties.attributes}"
        }
        check(case.interpretation == viewableInterpretation.interpretation) {
            "Case interpretation does not match viewable interpretation:\n\nCase interpretation: ${case.interpretation}\n\nViewable interpretation: ${viewableInterpretation.interpretation}"
        }
    }

    fun attributes() = viewProperties.attributes
    fun textGivenByRules() = viewableInterpretation.textGivenByRules
    fun verifiedText() = viewableInterpretation.verifiedText
    fun latestText() = viewableInterpretation.latestText()
    fun diffList() = viewableInterpretation.diffList
}

object ViewableCaseSerializer : KSerializer<ViewableCase> {
    private val caseSerializer = RDRCase.serializer()
    private val propsSerializer = CaseViewProperties.serializer()
    private val interpSerializer = ViewableInterpretation.serializer()
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("ViewableInterpretation") {
            element("case", caseSerializer.descriptor)
            element("props", propsSerializer.descriptor)
            element("interp", interpSerializer.descriptor)
        }

    override fun serialize(encoder: Encoder, value: ViewableCase) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, caseSerializer, value.case)
            encodeSerializableElement(descriptor, 1, propsSerializer, value.viewProperties)
            encodeSerializableElement(descriptor, 2, interpSerializer, value.viewableInterpretation)
        }
    }

    override fun deserialize(decoder: Decoder): ViewableCase {
        var case = RDRCase(CaseId())
        var props = CaseViewProperties()
        var interp = ViewableInterpretation()
        decoder.decodeStructure(descriptor) {
            // Loop label needed so that break statement works in js.
            parseLoop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> case = decodeSerializableElement(descriptor, 0, caseSerializer)
                    1 -> props = decodeSerializableElement(descriptor, 1, propsSerializer)
                    2 -> interp = decodeSerializableElement(descriptor, 2, interpSerializer)
                    CompositeDecoder.DECODE_DONE -> break@parseLoop
                    else -> error("Unexpected index: $index")
                }
            }
        }
        return ViewableCase(case, props, interp)
    }
}
