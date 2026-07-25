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
        processedNames shouldContainExactlyInAnyOrder listOf("Lindsay", "Sam")

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
    fun `Sam case has the expected attributes for the derived-attributes demo`() {
        DemoSampleBuilder(endpoint).setupCases()

        val sam = endpoint.kb.getProcessedCaseByName("Sam")
        val attributeNames = sam.attributes.map { it.name }
        attributeNames shouldContainExactlyInAnyOrder listOf("HbA1c", "Height", "Weight", "Age", "Sex")

        // HbA1c is out of range so that suggestion prioritisation surfaces it.
        val hba1c = endpoint.kb.attributeManager.getOrCreate("HbA1c")
        val hba1cResult = sam.getLatest(hba1c).shouldNotBeNull()
        hba1cResult.value.text shouldBe "7.8"
        hba1cResult.referenceRange.shouldNotBeNull().run {
            lowerString shouldBe "4.0"
            upperString shouldBe "6.0"
        }
        hba1cResult.units shouldBe " %"

        // Height and Weight give BMI = 98 / (1.78 * 1.78) = 30.93.
        val height = endpoint.kb.attributeManager.getOrCreate("Height")
        val heightResult = sam.getLatest(height).shouldNotBeNull()
        heightResult.value.text shouldBe "1.78"
        heightResult.units shouldBe " m"

        val weight = endpoint.kb.attributeManager.getOrCreate("Weight")
        val weightResult = sam.getLatest(weight).shouldNotBeNull()
        weightResult.value.text shouldBe "98"
        weightResult.units shouldBe " kg"

        val age = endpoint.kb.attributeManager.getOrCreate("Age")
        sam.getLatest(age).shouldNotBeNull().value.text shouldBe "54"

        val sex = endpoint.kb.attributeManager.getOrCreate("Sex")
        sam.getLatest(sex).shouldNotBeNull().value.text shouldBe "M"
    }

    @Test
    fun `setupCases is the only public entry point and produces a clean rule tree`() {
        DemoSampleBuilder(endpoint).setupCases()

        // No rules are seeded; the demo expects the user to build them live.
        endpoint.kb.ruleTree.size() shouldBe 1
        endpoint.kb.allProcessedCases() shouldHaveSize 2
        endpoint.kb.allCornerstoneCases() shouldHaveSize 1
    }
}
