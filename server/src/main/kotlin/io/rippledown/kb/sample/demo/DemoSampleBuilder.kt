package io.rippledown.kb.sample.demo

import io.rippledown.kb.sample.defaultDate
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.ReferenceRange
import io.rippledown.model.Result
import io.rippledown.model.Value
import io.rippledown.server.KBEndpoint

const val DEMO_SAMPLE_DESCRIPTION = """
    # Demo KB

A small demonstration KB used to show two OpenRDR features:

  1. Building a rule whose condition is expressed in a non-English
     language (Spanish), via the **Lindsay** waiting case.
  2. Reviewing a cornerstone case when adding a comment, via the
     **Jane** cornerstone case which shares Lindsay's attributes
     (Glucose, Pregnant, Age) but with different values.
"""

/**
 * Builds the contents of the [io.rippledown.sample.SampleKB.DEMO] sample KB.
 *
 * Cases are deliberately small (three attributes) so suggestion lookups
 * complete quickly and the demo stays responsive.
 */
class DemoSampleBuilder(private val kbe: KBEndpoint) {

    fun setupCases() {
        kbe.setDescription(DEMO_SAMPLE_DESCRIPTION)
        addLindsayWaitingCase()
        addJaneCornerstoneCase()
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
     * Jane: a cornerstone case with the same attributes as Lindsay
     * (Glucose, Pregnant, Age) but different values, so the cornerstone
     * navigation demo can run quickly without loading large pathology
     * panels.
     */
    private fun addJaneCornerstoneCase() {
        val attributes = kbe.kb.attributeManager
        val builder = RDRCaseBuilder()
        val glucose = attributes.getOrCreate("Glucose")
        val pregnant = attributes.getOrCreate("Pregnant")
        val age = attributes.getOrCreate("Age")
        builder.addResult(
            glucose,
            defaultDate,
            Result(Value("4.8"), ReferenceRange(null, "5.1"), " mmol/L")
        )
        builder.addValue(pregnant, defaultDate, "N")
        builder.addValue(age, defaultDate, "35")
        kbe.kb.addCornerstoneCase(builder.build("Jane"))
    }
}
