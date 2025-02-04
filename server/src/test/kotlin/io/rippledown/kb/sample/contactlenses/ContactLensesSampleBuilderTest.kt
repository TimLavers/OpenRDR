package io.rippledown.kb.sample.contactlenses

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.rippledown.kb.sample.SampleBuilderTest
import io.rippledown.kb.sample.contactlenses.ContactLensesRulesBuilder.Companion.AgeName
import io.rippledown.kb.sample.contactlenses.ContactLensesRulesBuilder.Companion.AstigmatismName
import io.rippledown.kb.sample.contactlenses.ContactLensesRulesBuilder.Companion.PrescriptionName
import io.rippledown.kb.sample.contactlenses.ContactLensesRulesBuilder.Companion.TearProductionName
import kotlin.test.Test

class ContactLensesSampleBuilderTest: SampleBuilderTest() {
    @Test
    fun `set up cases`() {
        ContactLensesSampleBuilder(endpoint).setupCases()
        checkAttributes()
        checkCases()
        endpoint.kb.ruleTree.size() shouldBe 1
        endpoint.description() shouldBe CONTACT_LENSES_CASES_DESCRIPTION
    }

    @Test
    fun `build rules`() {
        ContactLensesSampleBuilder(endpoint).buildRules()
        checkAttributes()
        checkCases()
        endpoint.kb.ruleTree.size() shouldBe 6
        val cases = endpoint.kb.allProcessedCases()
        fun interpretationForCase(index: Int): String {
            val interpretation = endpoint.kb.interpret(cases[index])
            interpretation.conclusionTexts().size shouldBe 1
            return interpretation.conclusionTexts().first()
        }
        endpoint.kb.interpret(cases[0]).conclusionTexts().size shouldBe 0
        interpretationForCase(1) shouldBe "soft"
        endpoint.kb.interpret(cases[2]).conclusionTexts().size shouldBe 0
        interpretationForCase(3) shouldBe "hard"

        endpoint.description() shouldBe CONTACT_LENSES_DESCRIPTION
    }

    private fun checkAttributes() {
        val attributesInOrder = endpoint.kb.caseViewManager.allInOrder().map { it.name }
        attributesInOrder shouldBe listOf(AgeName, PrescriptionName, AstigmatismName, TearProductionName)
    }

    private fun checkCases() {
        val caseNames = endpoint.kb.processedCaseIds().map { it.name }
        caseNames shouldHaveSize 24
        caseNames[0] shouldBe "Case1"
        caseNames[1] shouldBe "Case2"
        caseNames[23] shouldBe "Case24"
    }
}