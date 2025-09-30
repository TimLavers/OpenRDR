package io.rippledown.kb

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.rule.ChangeTreeToAddConclusion
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.utils.defaultDate
import kotlin.test.BeforeTest
import kotlin.test.Test

class KBExemptCornerstoneTest {
    private lateinit var kb: KB

    @BeforeTest
    fun setup() {
        val kbInfo = KBInfo("id123", "Bondi")
        kb = createKB(kbInfo)
    }

    @Test
    fun `should return an empty cornerstone status when the only cornerstone is exempted`() {
        //Given
        val cc1 = kb.addCornerstoneCase(createCase("Case1"))
        val vcc1 = kb.viewableCase(cc1)
        val sessionCase = createCase("Case3")

        //When
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))
        val currentCCStatus = CornerstoneStatus(vcc1, 0, 1)
        withClue("sanity check") {
            kb.cornerstoneStatus(vcc1) shouldBe currentCCStatus
        }
        val ccStatus = kb.exemptCornerstone(0)

        //Then
        ccStatus shouldBe CornerstoneStatus()
    }

    @Test
    fun `after exempting a cornerstone, should return a cornerstone status with the next cornerstone if there is one`() {
        //Given
        val cc1 = kb.addCornerstoneCase(createCase("Case1"))
        val cc2 = kb.addCornerstoneCase(createCase("Case2"))
        kb.addCornerstoneCase(createCase("Case3"))
        val vcc1 = kb.viewableCase(cc1)
        val vcc2 = kb.viewableCase(cc2)
        val sessionCase = createCase("Session")

        //When
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        val currentCCStatus = CornerstoneStatus(vcc1, 0, 3)
        withClue("sanity check") {
            kb.cornerstoneStatus(vcc1) shouldBe currentCCStatus
        }
        val ccStatus = kb.exemptCornerstone(0)

        //Then
        ccStatus shouldBe CornerstoneStatus(vcc2, 0, 2)
    }

    @Test
    fun `after exempting the last cornerstone, should return a cornerstone status with the previous cornerstone`() {
        //Given
        val cc1 = kb.addCornerstoneCase(createCase("Case1"))
        val cc2 = kb.addCornerstoneCase(createCase("Case2"))
        val cc3 = kb.addCornerstoneCase(createCase("Case3"))
        kb.viewableCase(cc1)
        val vcc2 = kb.viewableCase(cc2)
        val vcc3 = kb.viewableCase(cc3)
        val sessionCase = createCase("Session")

        //When
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        val currentCCStatus = CornerstoneStatus(vcc3, 2, 3)
        withClue("sanity check") {
            kb.cornerstoneStatus(vcc3) shouldBe currentCCStatus
        }
        val ccStatus = kb.exemptCornerstone(2)

        //Then
        ccStatus shouldBe CornerstoneStatus(vcc2, 1, 2)
    }

    private fun glucose() = kb.attributeManager.getOrCreate("Glucose")

    private fun createCase(caseName: String, glucoseValue: String = "0.667", id: Long? = null): RDRCase {
        with(RDRCaseBuilder()) {
            addValue(glucose(), defaultDate, glucoseValue)
            return build(caseName, id)
        }
    }

    private fun createKB(kbInfo: KBInfo) = KB(InMemoryKB(kbInfo))
}