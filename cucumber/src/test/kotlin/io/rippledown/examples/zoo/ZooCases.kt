package io.rippledown.examples.zoo

import io.rippledown.integration.proxy.ConfiguredTestData
import io.rippledown.model.AttributeFactory
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.defaultDate

class ZooCases(attributeFactory: AttributeFactory) {
    private val hair= attributeFactory.create("hair")
    private val feathers= attributeFactory.create("feathers")
    private val eggs= attributeFactory.create("eggs")
    private val milk= attributeFactory.create("milk")
    private val airborne= attributeFactory.create("airborne")
    private val aquatic	= attributeFactory.create("aquatic")
    private val predator= attributeFactory.create("predator")
    private val toothed	= attributeFactory.create("toothed")
    private val backbone= attributeFactory.create("backbone")
    private val breathes= attributeFactory.create("breathes")
    private val venomous= attributeFactory.create("venomous")
    private val fins= attributeFactory.create("fins")
    private val legs= attributeFactory.create("legs")
    private val tail= attributeFactory.create("tail")
    private val domestic= attributeFactory.create("domestic")
    private val catsize	= attributeFactory.create("catsize")

    private val indexToAttribute = mapOf(
        1 to hair,
        2 to feathers,
        3 to eggs,
        4 to milk,
        5 to airborne,
        6 to aquatic,
        7 to predator,
        8 to toothed,
        9 to backbone,
        10 to breathes,
        11 to venomous,
        12 to fins,
        13 to legs,
        14 to tail,
        15 to domestic,
        16 to catsize
    )

    fun attributesInOrder() = indexToAttribute.toSortedMap().map { it.value }

    fun cases(): List<RDRCase> {
        val result = mutableListOf<RDRCase>()
        val zooCasesFile = ConfiguredTestData.testDataFile("zoo/zoo.data")
        zooCasesFile.readLines().forEach {
            result.add(parseCase(it))
        }
        return result
    }

    private fun parseCase(data: String): RDRCase {
        // Values in the cases are true or false, represented
        // in the data set by 0 or 1, except for the legs,
        // attribute 13, which has integer values.
        fun convertValue(index: Int, rawValue: String): String {
            if (index == 13) {
                return rawValue
            }
            return if (rawValue == "0") "false" else "true"
        }
        val items = data.split(",")
        val caseName = items[0]
        val caseBuilder = RDRCaseBuilder()
        indexToAttribute.forEach { (index, attribute) ->
            val rawValue = items[index]
            caseBuilder.addValue(attribute, defaultDate, convertValue(index, rawValue))
        }
        return caseBuilder.build(caseName)
    }
}