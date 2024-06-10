package io.rippledown.kb.sample.zoo

import io.rippledown.kb.AttributeManager
import io.rippledown.kb.sample.defaultDate
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import java.io.BufferedReader
import java.io.InputStreamReader

class ZooCases(attributeFactory: AttributeManager) {
    private val hair= attributeFactory.getOrCreate("hair")
    private val feathers= attributeFactory.getOrCreate("feathers")
    private val eggs= attributeFactory.getOrCreate("eggs")
    private val milk= attributeFactory.getOrCreate("milk")
    private val airborne= attributeFactory.getOrCreate("airborne")
    private val aquatic	= attributeFactory.getOrCreate("aquatic")
    private val predator= attributeFactory.getOrCreate("predator")
    private val toothed	= attributeFactory.getOrCreate("toothed")
    private val backbone= attributeFactory.getOrCreate("backbone")
    private val breathes= attributeFactory.getOrCreate("breathes")
    private val venomous= attributeFactory.getOrCreate("venomous")
    private val fins= attributeFactory.getOrCreate("fins")
    private val legs= attributeFactory.getOrCreate("legs")
    private val tail= attributeFactory.getOrCreate("tail")
    private val domestic= attributeFactory.getOrCreate("domestic")
    private val catsize	= attributeFactory.getOrCreate("catsize")

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
        val zooData = this::class.java.getResourceAsStream("/zoo/zoo.data")!!
        BufferedReader(InputStreamReader(zooData)).lines().forEach {
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