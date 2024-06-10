package io.rippledown.kb.sample.vltsh

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.rippledown.kb.sample.SampleBuilderTest
import kotlin.test.Test

class TSHSampleBuilderTest: SampleBuilderTest() {
    @Test
    fun `set up cases`() {
        TSHSampleBuilder(endpoint).setupTSHSampleCases()
        checkAttributes()
        checkCases()
        endpoint.kb.ruleTree.size() shouldBe 1
    }

    @Test
    fun `build rules`() {
        TSHSampleBuilder(endpoint).buildTSHRules()
        checkAttributes()
        checkCases()
        endpoint.kb.ruleTree.size() shouldBe 34
        val cases = endpoint.kb.allProcessedCases()
        fun interpretationForCase(index: Int): String {
            val interpretation = endpoint.kb.interpret(cases[index])
            interpretation.conclusionTexts().size shouldBe 1
            return interpretation.conclusionTexts().first()
        }
        interpretationForCase(0) shouldBe "Normal T4 and TSH are consistent with a euthyroid state."
        interpretationForCase(1) shouldBe "Normal TSH is consistent with a euthyroid state."
        interpretationForCase(2) shouldBe "A mildly reduced FT4 with a normal TSH may be due to non-thyroidal illness or pituitary hypothyroidism."
    }

    private fun checkAttributes() {
        val attributesInOrder = endpoint.kb.caseViewManager.allInOrder().map { it.name }
        attributesInOrder shouldBe listOf(
            "Sex",
            "Age",
            "TSH",
            "Free T4",
            "Free T3",
            "TPO Antibodies",
            "Thyroglobulin",
            "Anti-Thyroglobulin",
            "Sodium",
            "Potassium",
            "Bicarbonate",
            "Urea",
            "Creatinine",
            "eGFR",
            "Patient Location",
            "Tests",
            "Clinical Notes"
        )
    }

    private fun checkCases() {
        val caseNames = endpoint.kb.processedCaseIds().map { it.name }
        caseNames shouldHaveSize 34
        caseNames[0] shouldBe "1.4.1"
        caseNames[1] shouldBe "1.4.2"
        caseNames[33] shouldBe "1.4.35"
    }
}