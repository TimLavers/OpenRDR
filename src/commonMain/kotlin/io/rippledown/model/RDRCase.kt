package io.rippledown.model

import io.rippledown.model.rule.RuleSummary
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*

class RDRCaseBuilder {
    private val caseData: MutableMap<TestEvent, TestResult> = mutableMapOf()

    fun addValue(attribute: String, date: Long, value: String) {
        val result = TestResult(value)
        addResult(attribute, date, result)
    }

    fun addResult(attribute: String, date: Long, result: TestResult) {
        addResult(Attribute(attribute), date, result)
    }
    fun addResult(attribute: Attribute, date: Long, result: TestResult) {
        val testEvent = TestEvent(attribute, date)
        caseData[testEvent] = result
    }

    fun build(name: String): RDRCase {
        return RDRCase(name, caseData)
    }
}

object RDRCaseSerializer : KSerializer<RDRCase> {
    private val mapSerializer = MapSerializer(TestEvent.serializer(), TestResult.serializer())
    private val interpretationRulesSerializer = SetSerializer(RuleSummary.serializer())
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("RDRCase") {
            element("name", String.serializer().descriptor)
            element("data", mapSerializer.descriptor)
            element("rules", interpretationRulesSerializer.descriptor)
        }

    override fun serialize(encoder: Encoder, value: RDRCase) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.name)
            encodeSerializableElement(descriptor, 1, mapSerializer, value.data)
            encodeSerializableElement(descriptor, 2, interpretationRulesSerializer, value.interpretation.ruleSummaries())
        }
    }

    override fun deserialize(decoder: Decoder): RDRCase {
        var name = ""
        var map: Map<TestEvent, TestResult> = emptyMap()
        var rules: Set<RuleSummary> = emptySet()
        decoder.decodeStructure(descriptor) {
            // Loop label needed so that break statement works in js.
            parseLoop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> name = decodeStringElement(descriptor, 0)
                    1 -> map = decodeSerializableElement(descriptor, 1, mapSerializer)
                    2 -> rules = decodeSerializableElement(descriptor, 2, interpretationRulesSerializer)
                    CompositeDecoder.DECODE_DONE -> break@parseLoop
                    else -> error("Unexpected index: $index")
                }
            }
        }
        val case = RDRCase(name, map)
        rules.forEach { case.interpretation.add(it)}
        return case
    }
}

/**
 * An RDRCase is a set of (TestEvent, TestResult) pairs in which
 * no two TestEvents have the same attribute and date.
 * The case can be organised as a table of TestResults,
 * in which columns represent TestResults with
 * the same date and rows represent results for
 * the same attribute. In this arrangement,
 * a blank TestResult is given on dates at which
 * there was no TestEvent for an Attribute.
 */
@Serializable(RDRCaseSerializer::class)
data class RDRCase(val name: String = "", val data: Map<TestEvent, TestResult> = emptyMap()) {
    val dates: List<Long>
    val attributes: Set<Attribute>
    private val dateToEpisode: Map<Long, Map<Attribute, TestResult>>
    val interpretation = createInterpretation()

    init {
        val uniqueDates = mutableSetOf<Long>()
        data.keys.forEach { uniqueDates.add(it.date) }
        dates = uniqueDates.sorted()
        attributes = data.keys.map { it.attribute }.toSet()
        val dateToEpisodeMutable = mutableMapOf<Long, Map<Attribute, TestResult>>()

        dates.forEach {
            val attributeMap = mutableMapOf<Attribute, TestResult>()

            attributes.forEach { attribute ->
                val key = TestEvent(attribute, it)
                val result = data[key] ?: TestResult("")
                attributeMap[attribute] = result
            }
            dateToEpisodeMutable[it] = attributeMap.toMap()
        }
        dateToEpisode = dateToEpisodeMutable.toMap()
    }

    fun values(attributeName: String): List<TestResult>? {
        val attribute = Attribute(attributeName)
        if (!attributes.contains(attribute)) {
            return null
        }
        val result = mutableListOf<TestResult>()
        dates.forEach {
            result.add(dateToEpisode[it]!![attribute]!!)
        }
        return result
    }

    fun resultsFor(attribute: Attribute): ResultsList? {
        if (!attributes.contains(attribute)) {
            return null
        }
        return ResultsList(values(attribute.name)!!)
    }

    fun get(attributeName: String): TestResult? {
        return getLatest(Attribute(attributeName))
    }

    fun getLatest(attribute: Attribute): TestResult? {
        return dateToEpisode[dates.last()]!![attribute]
    }

    fun resetInterpretation() {
        interpretation.reset()
    }

    private fun createInterpretation(): Interpretation {
        return Interpretation(CaseId(name, name))
    }

    fun setVerifiedText(text: String) {
        interpretation.verifiedText = text
    }
}