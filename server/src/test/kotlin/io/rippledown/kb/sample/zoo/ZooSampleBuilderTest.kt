package io.rippledown.kb.sample.zoo

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.rippledown.kb.sample.SampleBuilderTest
import io.rippledown.model.AttributeKind
import kotlin.test.Test

class ZooSampleBuilderTest: SampleBuilderTest() {
    @Test
    fun `set up cases`() {
        ZooSampleBuilder(endpoint).setupCases()
        checkAttributes()
        checkCases()
        endpoint.kb.ruleTree.size() shouldBe 1
        endpoint.description() shouldBe ZOO_CASES_SAMPLE_DESCRIPTION
    }

    @Test
    fun `build rules`() {
        ZooSampleBuilder(endpoint).buildRules()
        checkAttributes()
        checkCases()
        endpoint.kb.ruleTree.size() shouldBe 18
        val cases = endpoint.kb.allProcessedCases()
        fun interpretationForCase(index: Int): String {
            val comments = endpoint.kb.viewableCase(cases[index]).viewableInterpretation.renderedComments
            comments.size shouldBe 1
            return comments.first().text
        }
        interpretationForCase(0) shouldBe "mammal"
        interpretationForCase(1) shouldBe "mammal"
        interpretationForCase(2) shouldBe "fish"
        endpoint.description() shouldBe ZOO_SAMPLE_DESCRIPTION
    }

    private fun checkAttributes() {
        // Comment attributes are created by the rules; the case view order of the external attributes is unchanged.
        val attributesInOrder = endpoint.kb.caseViewManager.allInOrder()
            .filter { it.kind == AttributeKind.EXTERNAL }.map { it.name }
        attributesInOrder shouldBe listOf(
            "hair",
            "feathers",
            "eggs",
            "milk",
            "airborne",
            "aquatic",
            "predator",
            "toothed",
            "backbone",
            "breathes",
            "venomous",
            "fins",
            "legs",
            "tail",
            "domestic",
            "catsize"
        )
    }

    private fun checkCases() {
        val caseNames = endpoint.kb.processedCaseIds().map { it.name }
        caseNames shouldHaveSize 101
        caseNames[0] shouldBe "aardvark"
        caseNames[1] shouldBe "antelope"
        caseNames[2] shouldBe "bass"
    }
}