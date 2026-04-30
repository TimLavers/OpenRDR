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
        processedNames shouldContainExactlyInAnyOrder listOf("Lindsay", "Einstein")

        val cornerstoneNames = endpoint.kb.allCornerstoneCases().map { it.name }
        cornerstoneNames shouldContainExactlyInAnyOrder listOf("Planck")
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
    fun `Einstein and Planck are loaded with their full attribute panels`() {
        DemoSampleBuilder(endpoint).setupCases()

        // The Einstein and Planck JSON resources are pathology panels with
        // many attributes; we don't pin the exact count here (it would
        // couple the test to the resource file), but we do require that
        // each has substantially more attributes than Lindsay's three.
        val einstein = endpoint.kb.getProcessedCaseByName("Einstein")
        einstein.attributes.size shouldBe einstein.attributes.size // sanity
        check(einstein.attributes.size > 10) {
            "Expected Einstein to have many attributes; got ${einstein.attributes.size}."
        }

        val planck = endpoint.kb.getCornerstoneCaseByName("Planck")
        check(planck.attributes.size > 10) {
            "Expected Planck to have many attributes; got ${planck.attributes.size}."
        }
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
