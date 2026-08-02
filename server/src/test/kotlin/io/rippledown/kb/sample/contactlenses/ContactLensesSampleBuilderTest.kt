package io.rippledown.kb.sample.contactlenses

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.rippledown.kb.sample.SampleBuilderTest
import io.rippledown.kb.sample.contactlenses.ContactLensesRulesBuilder.Companion.AgeName
import io.rippledown.kb.sample.contactlenses.ContactLensesRulesBuilder.Companion.AstigmatismName
import io.rippledown.kb.sample.contactlenses.ContactLensesRulesBuilder.Companion.PrescriptionName
import io.rippledown.kb.sample.contactlenses.ContactLensesRulesBuilder.Companion.TearProductionName
import io.rippledown.model.AttributeKind
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
        fun commentsForCase(index: Int): List<String> =
            endpoint.kb.viewableCase(cases[index]).viewableInterpretation.renderedComments.map { it.text }
        commentsForCase(0) shouldBe emptyList()
        commentsForCase(1) shouldBe listOf("soft")
        commentsForCase(2) shouldBe emptyList()
        commentsForCase(3) shouldBe listOf("hard")

        endpoint.description() shouldBe CONTACT_LENSES_DESCRIPTION
    }

    private fun checkAttributes() {
        // Comment attributes are created by the rules; the case view order of the external attributes is unchanged.
        val attributesInOrder = endpoint.kb.caseViewManager.allInOrder()
            .filter { it.kind == AttributeKind.EXTERNAL }.map { it.name }
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