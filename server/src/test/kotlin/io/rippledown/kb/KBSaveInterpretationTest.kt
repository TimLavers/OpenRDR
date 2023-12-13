package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.defaultDate
import io.rippledown.persistence.inmemory.InMemoryKB
import kotlin.test.BeforeTest
import kotlin.test.Test

class KBSaveInterpretationTest {
    private lateinit var kb: KB

    @BeforeTest
    fun setup() {
        val kbInfo = KBInfo("id123", "Blah")
        kb = createKB(kbInfo)
    }

    @Test
    fun `interpreting a case should not overwrite the verified text`() {
        //Given
        val verifiedText = "Go to Bondi"
        val case = createCase()
        val viewableCase = kb.viewableCase(case)
        viewableCase.viewableInterpretation.verifiedText = verifiedText
        kb.saveInterpretation(viewableCase.viewableInterpretation)
        viewableCase.latestText() shouldBe verifiedText

        //When
        val reinterpreted = kb.viewableCase(case)

        //Then
        reinterpreted.latestText() shouldBe verifiedText
    }

    @Test
    fun `should save the verified text of an interpretation`() {
        //Given
        val case = createCase()
        val verified = "Bondi or bust"
        val interp = kb.viewableCase(case).viewableInterpretation.apply { verifiedText = verified }

        //When
        kb.saveInterpretation(interp)

        //Then
        kb.viewableCase(case).viewableInterpretation.verifiedText shouldBe verified
    }

    @Test
    fun `should save the conclusions corresponding to the verified text of an interpretation`() {
        //Given
        val case = createCase()
        val verified = "Bondi or bust. And bring a towel."
        val interp = kb.viewableCase(case).viewableInterpretation.apply { verifiedText = verified }

        //When
        kb.saveInterpretation(interp)

        //Then
        val comments = kb.interpretationViewManager.allInOrder().map { it.text }
        comments shouldBe listOf("Bondi or bust.", "And bring a towel.")
    }

    @Test
    fun `the conclusions saved from a subsequent interpretation should not overwrite conclusions saved from an earlier interpretation`() {
        //Given
        val case1 = createCase("Case1", id = 1)
        val case2 = createCase("Case2", id = 2)
        val verified1 = "Bondi or bust. And bring a towel."
        val interp1 = kb.viewableCase(case1).viewableInterpretation.apply { verifiedText = verified1 }
        kb.saveInterpretation(interp1)

        //When
        val verified2 = "Malabar or bust. And bring goggles."
        val interp2 = kb.viewableCase(case2).viewableInterpretation.apply { verifiedText = verified2 }
        kb.saveInterpretation(interp2)

        //Then
        val comments = kb.interpretationViewManager.allInOrder().map { it.text }
        comments shouldBe listOf("Bondi or bust.", "And bring a towel.", "Malabar or bust.", "And bring goggles.")
    }

    @Test
    fun `should insert a new conclusion before an existing conclusion as specified in the verified text`() {
        //Given
        val case1 = createCase("Case1", id = 1)
        val case2 = createCase("Case2", id = 2)
        val verified1 = "Bondi or bust. And bring a towel."
        val interp1 = kb.viewableCase(case1).viewableInterpretation.apply { verifiedText = verified1 }
        kb.saveInterpretation(interp1)

        //When
        val verified2 = "Malabar or bust. And bring a towel. And bring goggles."
        val interp2 = kb.viewableCase(case2).viewableInterpretation.apply { verifiedText = verified2 }
        kb.saveInterpretation(interp2)

        //Then
        val comments = kb.interpretationViewManager.allInOrder().map { it.text }
        comments shouldBe listOf("Bondi or bust.", "Malabar or bust.", "And bring a towel.", "And bring goggles.")
    }

    private fun createCase(caseName: String = "case", id: Long = 0) =
        with(RDRCaseBuilder()) {
            addValue(attribute(), defaultDate, "42")
            build(caseName, id)
        }

    private fun attribute() = kb.attributeManager.getOrCreate("wave height")

    private fun createKB(kbInfo: KBInfo) = KB(InMemoryKB(kbInfo))
}