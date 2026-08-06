package io.rippledown.kb.sample.demo

import io.rippledown.kb.sample.defaultDate
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.ReferenceRange
import io.rippledown.model.Result
import io.rippledown.model.Value
import io.rippledown.server.KBEndpoint

const val DEMO_SAMPLE_DESCRIPTION = """
    # Demo KB

A small demonstration KB used to show OpenRDR features:

  1. Building a rule whose condition is expressed in a non-English
     language (Spanish), via the **Lindsay** waiting case.
  2. Reviewing a cornerstone case when adding a comment, via the
     **Jane** cornerstone case which shares Lindsay's attributes
     (Glucose, Pregnant, Age) but with different values.
  3. Derived attributes, repeat inferencing and the AI-generated report,
     via the **Taylor** waiting case (HbA1c out of range; Height and Weight
     for a BMI formula).
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
        addTaylorWaitingCase()
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

    /**
     * Taylor: a waiting case for the derived-attributes demo (see
     * `packaging/README-demo.txt`). HbA1c is out of range so that
     * suggestion prioritisation surfaces it when building the
     * "Diabetes status" rule; Height and Weight give BMI
     * 98 / (1.78 * 1.78) = 30.93, just over the obesity threshold of 30;
     * Age and Sex are included so the formula can be extended (e.g. BMR)
     * if the demo wants a gender-dependent example.
     */
    private fun addTaylorWaitingCase() {
        val attributes = kbe.kb.attributeManager
        val builder = RDRCaseBuilder()
        val hba1c = attributes.getOrCreate("HbA1c")
        val height = attributes.getOrCreate("Height")
        val weight = attributes.getOrCreate("Weight")
        val age = attributes.getOrCreate("Age")
        val sex = attributes.getOrCreate("Sex")
        builder.addResult(
            hba1c,
            defaultDate,
            Result(Value("7.8"), ReferenceRange("4.0", "6.0"), " %")
        )
        builder.addResult(
            height,
            defaultDate,
            Result(Value("1.78"), null, " m")
        )
        builder.addResult(
            weight,
            defaultDate,
            Result(Value("98"), null, " kg")
        )
        builder.addValue(age, defaultDate, "54")
        builder.addValue(sex, defaultDate, "F")
        kbe.kb.addProcessedCase(builder.build("Taylor"))
    }
}
