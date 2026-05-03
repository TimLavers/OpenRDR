package io.rippledown.kb.sample.demo

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.rippledown.kb.sample.SampleBuilderTest
import kotlin.test.Test

class DemoSampleBuilderTest : SampleBuilderTest() {

    @Test
    fun `set up cases`() {
        DemoSampleBuilder(endpoint).setupCases()

        endpoint.description() shouldBe DEMO_SAMPLE_DESCRIPTION
        endpoint.kb.ruleTree.size() shouldBe 1

        val processedNames = endpoint.kb.allProcessedCases().map { it.name }
        processedNames shouldContainExactlyInAnyOrder listOf("Lindsay")

        val cornerstoneNames = endpoint.kb.allCornerstoneCases().map { it.name }
        cornerstoneNames shouldContainExactlyInAnyOrder listOf("Jane")
    }

    @Test
    fun `Lindsay case has the expected attributes for the Spanish demo`() {
        DemoSampleBuilder(endpoint).setupCases()

        val lindsay = endpoint.kb.getProcessedCaseByName("Lindsay")
        val attributeNames = lindsay.attributes.map { it.name }
        attributeNames shouldContainExactlyInAnyOrder listOf("Glucose", "Pregnant", "Age")

        val glucose = endpoint.kb.attributeManager.getOrCreate("Glucose")
        val glucoseResult = lindsay.getLatest(glucose).shouldNotBeNull()
        glucoseResult.value.text shouldBe "5.2"
        glucoseResult.referenceRange.shouldNotBeNull().run {
            lowerString shouldBe null
            upperString shouldBe "5.1"
        }
        glucoseResult.units shouldBe " mmol/L"

        val pregnant = endpoint.kb.attributeManager.getOrCreate("Pregnant")
        lindsay.getLatest(pregnant).shouldNotBeNull().value.text shouldBe "Y"

        val age = endpoint.kb.attributeManager.getOrCreate("Age")
        lindsay.getLatest(age).shouldNotBeNull().value.text shouldBe "21"
    }

    @Test
    fun `Jane cornerstone case shares Lindsay's attributes with different values`() {
        DemoSampleBuilder(endpoint).setupCases()

        val jane = endpoint.kb.getCornerstoneCaseByName("Jane")
        val attributeNames = jane.attributes.map { it.name }
        attributeNames shouldContainExactlyInAnyOrder listOf("Glucose", "Pregnant", "Age")

        val glucose = endpoint.kb.attributeManager.getOrCreate("Glucose")
        val glucoseResult = jane.getLatest(glucose).shouldNotBeNull()
        glucoseResult.value.text shouldBe "4.8"
        glucoseResult.referenceRange.shouldNotBeNull().run {
            lowerString shouldBe null
            upperString shouldBe "5.1"
        }
        glucoseResult.units shouldBe " mmol/L"

        val pregnant = endpoint.kb.attributeManager.getOrCreate("Pregnant")
        jane.getLatest(pregnant).shouldNotBeNull().value.text shouldBe "N"

        val age = endpoint.kb.attributeManager.getOrCreate("Age")
        jane.getLatest(age).shouldNotBeNull().value.text shouldBe "35"
    }

    @Test
    fun `setupCases is the only public entry point and produces a clean rule tree`() {
        DemoSampleBuilder(endpoint).setupCases()

        // No rules are seeded; the demo expects the user to build them live.
        endpoint.kb.ruleTree.size() shouldBe 1
        endpoint.kb.allProcessedCases() shouldHaveSize 1
        endpoint.kb.allCornerstoneCases() shouldHaveSize 1
    }
}
