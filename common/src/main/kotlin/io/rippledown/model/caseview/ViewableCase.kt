package io.rippledown.model.caseview

import io.rippledown.model.AttributeKind
import io.rippledown.model.CaseId
import io.rippledown.model.RDRCase
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.model.rule.AiBoolean
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*

@Serializable(ViewableCaseSerializer::class)
data class ViewableCase(
    val case: RDRCase,
    val viewProperties: CaseViewProperties = CaseViewProperties(),
    var viewableInterpretation: ViewableInterpretation = ViewableInterpretation(case.interpretation),
    val aiProvenance: Map<Int, AiProvenance> = emptyMap(),
    val incomplete: IncompleteInterpretation? = null
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
    fun latestText() = viewableInterpretation.latestText()

    /**
     * Non-comment derived attribute values for display in the Derived attributes
     * panel. Each entry includes the attribute name, the value assigned by the
     * KB for this case, the formula text, and the condition texts from root
     * for the rule that assigned the value (for the tooltip).
     */
    fun derivedValues(): List<DerivedValueInfo> {
        incomplete?.let { failure ->
            return failure.attributeName?.let { name ->
                listOf(DerivedValueInfo(name, "", "", emptyList(), DerivedValueStatus.Failed(failure.reason)))
            }.orEmpty()
        }
        return viewableInterpretation.assignments()
            .filter { it.attribute.kind == AttributeKind.DERIVED }
            .map { assignment ->
                val value = case.latestValue(assignment.attribute)
                val definition = assignment.expression as? AiBoolean
                DerivedValueInfo(
                    name = assignment.attribute.name,
                    value = value ?: "",
                    formula = assignment.expression.asText(),
                    conditions = viewableInterpretation.conditionsForAssignment(assignment),
                    status = if (definition != null && value == null) DerivedValueStatus.NoInput(definition.sourceAttribute.name)
                        else DerivedValueStatus.Evaluated,
                    provenance = aiProvenance[assignment.attribute.id]
                )
            }
            .sortedBy { it.name }
    }
}

object ViewableCaseSerializer : KSerializer<ViewableCase> {
    private val caseSerializer = RDRCase.serializer()
    private val propsSerializer = CaseViewProperties.serializer()
    private val interpSerializer = ViewableInterpretation.serializer()
    private val provenanceSerializer = MapSerializer(Int.serializer(), AiProvenance.serializer())
    private val incompleteSerializer = IncompleteInterpretation.serializer()
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("ViewableInterpretation") {
            element("case", caseSerializer.descriptor)
            element("props", propsSerializer.descriptor)
            element("interp", interpSerializer.descriptor)
            element("aiProvenance", provenanceSerializer.descriptor, isOptional = true)
            element("incomplete", incompleteSerializer.descriptor, isOptional = true)
        }

    override fun serialize(encoder: Encoder, value: ViewableCase) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, caseSerializer, value.case)
            encodeSerializableElement(descriptor, 1, propsSerializer, value.viewProperties)
            encodeSerializableElement(descriptor, 2, interpSerializer, value.viewableInterpretation)
            if (value.aiProvenance.isNotEmpty()) encodeSerializableElement(descriptor, 3, provenanceSerializer, value.aiProvenance)
            value.incomplete?.let { encodeSerializableElement(descriptor, 4, incompleteSerializer, it) }
        }
    }

    override fun deserialize(decoder: Decoder): ViewableCase {
        var case = RDRCase(CaseId())
        var props = CaseViewProperties()
        var interp = ViewableInterpretation()
        var provenance = emptyMap<Int, AiProvenance>()
        var incomplete: IncompleteInterpretation? = null
        decoder.decodeStructure(descriptor) {
            // Loop label needed so that break statement works in js.
            parseLoop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> case = decodeSerializableElement(descriptor, 0, caseSerializer)
                    1 -> props = decodeSerializableElement(descriptor, 1, propsSerializer)
                    2 -> interp = decodeSerializableElement(descriptor, 2, interpSerializer)
                    3 -> provenance = decodeSerializableElement(descriptor, 3, provenanceSerializer)
                    4 -> incomplete = decodeSerializableElement(descriptor, 4, incompleteSerializer)
                    CompositeDecoder.DECODE_DONE -> break@parseLoop
                    else -> error("Unexpected index: $index")
                }
            }
        }
        return ViewableCase(case, props, interp, provenance, incomplete)
    }
}
