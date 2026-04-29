package io.rippledown.kb.sample.demo

import io.rippledown.kb.sample.defaultDate
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.ReferenceRange
import io.rippledown.model.Result
import io.rippledown.model.Value
import io.rippledown.model.external.ExternalCase
import io.rippledown.server.KBEndpoint
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

const val DEMO_SAMPLE_DESCRIPTION = """
    # Demo KB

A small demonstration KB used to show two OpenRDR features:

  1. Building a rule whose condition is expressed in a non-English
     language (Spanish), via the **Lindsay** waiting case.
  2. Reviewing cornerstone cases when adding a comment, via the
     **Einstein** waiting case and the **Planck** cornerstone case.

The Einstein and Planck cases are real-world pathology data with many
attributes; they showcase the targeted-suggestions list and the
cornerstone navigation chat flow.
"""

/**
 * Builds the contents of the [io.rippledown.sample.SampleKB.DEMO] sample KB.
 */
class DemoSampleBuilder(private val kbe: KBEndpoint) {

    fun setupCases() {
        kbe.setDescription(DEMO_SAMPLE_DESCRIPTION)
        addLindsayWaitingCase()
        addEinsteinWaitingCase()
        addPlanckCornerstoneCase()
    }

    /**
     * Lindsay: a single waiting case with three attributes, used to demo
     * adding a comment with a Spanish-language condition.
     *
     * Mirrors the data in
     * `cucumber/src/test/resources/requirements/chat/Build rules using
     * non-English languages.feature` (Spanish scenario).
     */
    private fun addLindsayWaitingCase() {
        val attributes = kbe.kb.attributeManager
        val builder = RDRCaseBuilder()
        val glucose = attributes.getOrCreate("Glucose")
        val pregnant = attributes.getOrCreate("Pregnant")
        val age = attributes.getOrCreate("Age")
        builder.addResult(
            glucose,
            defaultDate,
            Result(Value("5.2"), ReferenceRange(null, "5.1"), " mmol/L")
        )
        builder.addValue(pregnant, defaultDate, "Y")
        builder.addValue(age, defaultDate, "21")
        kbe.kb.addProcessedCase(builder.build("Lindsay"))
    }

    /**
     * Einstein: a complex pathology waiting case loaded from
     * `/demo/Einstein.json`. Used as the "current case" in the cornerstone
     * navigation demo.
     */
    private fun addEinsteinWaitingCase() {
        val external = readExternalCase("/demo/Einstein.json")
        kbe.processCase(external)
    }

    /**
     * Planck: a complex pathology cornerstone case loaded from
     * `/demo/Planck.json`. Used as the cornerstone surfaced when the user
     * starts to add a comment to Einstein.
     */
    private fun addPlanckCornerstoneCase() {
        val external = readExternalCase("/demo/Planck.json")
        kbe.addCornerstoneCase(external)
    }

    private fun readExternalCase(resourcePath: String): ExternalCase {
        val stream = this::class.java.getResourceAsStream(resourcePath)
            ?: error("Missing demo resource: $resourcePath")
        val text = BufferedReader(InputStreamReader(stream)).use { it.readText() }
        val json = Json { allowStructuredMapKeys = true }
        return json.decodeFromString(ExternalCase.serializer(), text)
    }
}
