package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.rule.ChangeTreeToAddConclusion
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.utils.defaultDate
import kotlin.test.BeforeTest
import kotlin.test.Test

class KBSelectCornerstoneTest {
    private lateinit var kb: KB

    @BeforeTest
    fun setup() {
        val kbInfo = KBInfo("id123", "Bondi")
        kb = createKB(kbInfo)
    }

    @Test
    fun `should select the first cornerstone case`() {
        //Given
        val cc1 = kb.addCornerstoneCase(createCase("Case1"))
        val vcc1 = kb.viewableCase(cc1)
        val sessionCase = createCase("Session")
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        //When
        val ccStatus = kb.selectCornerstoneCase(0)

        //Then
        ccStatus shouldBe CornerstoneStatus(vcc1, 0, 1)
    }

    @Test
    fun `should select the second cornerstone case`() {
        //Given
        kb.addCornerstoneCase(createCase("Case1"))
        val cc2 = kb.addCornerstoneCase(createCase("Case2"))
        kb.addCornerstoneCase(createCase("Case3"))
        val vcc2 = kb.viewableCase(cc2)
        val sessionCase = createCase("Session")
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        //When
        val ccStatus = kb.selectCornerstoneCase(1)

        //Then
        ccStatus shouldBe CornerstoneStatus(vcc2, 1, 3)
    }

    @Test
    fun `should select the last cornerstone case`() {
        //Given
        kb.addCornerstoneCase(createCase("Case1"))
        kb.addCornerstoneCase(createCase("Case2"))
        val cc3 = kb.addCornerstoneCase(createCase("Case3"))
        val vcc3 = kb.viewableCase(cc3)
        val sessionCase = createCase("Session")
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        //When
        val ccStatus = kb.selectCornerstoneCase(2)

        //Then
        ccStatus shouldBe CornerstoneStatus(vcc3, 2, 3)
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
