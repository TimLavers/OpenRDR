package io.rippledown.kb.sample.cancergenes

import io.rippledown.kb.AttributeManager
import io.rippledown.kb.sample.defaultDate
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.server.KBEndpoint
import java.io.BufferedReader
import java.io.InputStreamReader

class GenesCases(val attributeFactory: AttributeManager) {
    val gene = attributeFactory.getOrCreate("gene")
    val driverRole = attributeFactory.getOrCreate("driverRole")
    val driverInterp = attributeFactory.getOrCreate("driverInterp")
    val hotspot = attributeFactory.getOrCreate("hotspot")
    val biallelic = attributeFactory.getOrCreate("biallelic")
    val effects = attributeFactory.getOrCreate("effects")
    val codingEffect = attributeFactory.getOrCreate("codingEffect")
    val proteinImpact = attributeFactory.getOrCreate("proteinImpact")
    val codingImpact = attributeFactory.getOrCreate("codingImpact")
    val codon = attributeFactory.getOrCreate("codon")
    val hrdStatus = attributeFactory.getOrCreate("hrdStatus")
    val isHrdDrivingGene = attributeFactory.getOrCreate("isHrdDrivingGene")

    fun setAttributeOrder(kbe: KBEndpoint) {
        kbe.setAttributeOrder(
            listOf(
                gene,
                driverRole,
                driverInterp,
                hotspot,
                biallelic,
                effects,
                codingEffect,
                proteinImpact,
                codingImpact,
                codon,
                hrdStatus,
                isHrdDrivingGene
            )
        )
    }

    fun cases(): List<RDRCase> {
        val result = mutableListOf<RDRCase>()
        val zooData = this::class.java.getResourceAsStream("/genes/cases.csv")!!
        val fileLines = BufferedReader(InputStreamReader(zooData)).lines().toList()
        val positionToAttribute = mutableMapOf<Int, Attribute>()
        fileLines[0]!!.split(",").forEachIndexed { i, a ->
            if (i > 0) {
                positionToAttribute[i] = attributeFactory.getOrCreate(a)
            }
        }
        fileLines.drop(1).forEach {
            val caseBuilder = RDRCaseBuilder()
            val values = it.split(",")
            val attributeToValue = mutableMapOf<Attribute, String>()
            values.forEachIndexed { i, value ->
                if (i > 0) {
                    attributeToValue[positionToAttribute[i]!!] = value
                    caseBuilder.addValue(positionToAttribute[i]!!, defaultDate, value)
                }
            }
            result.add(caseBuilder.build(values[0]))
        }
        return result
    }

}