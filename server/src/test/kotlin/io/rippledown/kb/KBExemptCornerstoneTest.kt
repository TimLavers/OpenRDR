package io.rippledown.kb

import io.kotest.assertions.withClue
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

class KBExemptCornerstoneTest {
    private lateinit var kb: KB
    private lateinit var rsm: RuleSessionManager

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
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))
        val currentCCStatus = CornerstoneStatus(vcc1, 0, 1)
        withClue("sanity check") {
            rsm.cornerstoneStatus(vcc1) shouldBe currentCCStatus
        }
        val ccStatus = rsm.exemptCornerstone(0)

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
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        val currentCCStatus = CornerstoneStatus(vcc1, 0, 3)
        withClue("sanity check") {
            rsm.cornerstoneStatus(vcc1) shouldBe currentCCStatus
        }
        val ccStatus = rsm.exemptCornerstone(0)

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
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        val currentCCStatus = CornerstoneStatus(vcc3, 2, 3)
        withClue("sanity check") {
            rsm.cornerstoneStatus(vcc3) shouldBe currentCCStatus
        }
        val ccStatus = rsm.exemptCornerstone(2)

        //Then
        ccStatus shouldBe CornerstoneStatus(vcc2, 1, 2)
    }

    @Test
    fun `exemptCornerstoneCase should exempt the currently selected cornerstone`() {
        //Given
        kb.addCornerstoneCase(createCase("Case1"))
        kb.addCornerstoneCase(createCase("Case2"))
        kb.addCornerstoneCase(createCase("Case3"))
        val sessionCase = createCase("Session")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        //When - select Case3 (index 2), then exempt via exemptCornerstoneCase
        rsm.selectCornerstone(2)
        val ccStatus = rsm.exemptCornerstoneCase()

        //Then - Case3 was exempted, Case1 and Case2 remain, selected should be Case2 (coerced from index 2 to 1)
        ccStatus.numberOfCornerstones shouldBe 2
        ccStatus.cornerstoneToReview shouldBe kb.viewableCase(kb.allCornerstoneCases()[1])
    }

    @Test
    fun `exemptCornerstoneCase should exempt the first cornerstone when no selection has been made`() {
        //Given
        val cc1 = kb.addCornerstoneCase(createCase("Case1"))
        val cc2 = kb.addCornerstoneCase(createCase("Case2"))
        val vcc2 = kb.viewableCase(cc2)
        val sessionCase = createCase("Session")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        //When - no selectCornerstone call, so default is index 0
        val ccStatus = rsm.exemptCornerstoneCase()

        //Then - Case1 (index 0) was exempted, only Case2 remains
        ccStatus.numberOfCornerstones shouldBe 1
        ccStatus.cornerstoneToReview shouldBe vcc2
        ccStatus.indexOfCornerstoneToReview shouldBe 0
    }

    @Test
    fun `exemptCornerstoneCase should return empty status when last remaining cornerstone is exempted`() {
        //Given
        kb.addCornerstoneCase(createCase("Case1"))
        val cc2 = kb.addCornerstoneCase(createCase("Case2"))
        val sessionCase = createCase("Session")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        //When - select Case2 (index 1), exempt it, then exempt the remaining Case1
        rsm.selectCornerstone(1)
        rsm.exemptCornerstoneCase()
        // Now only Case1 remains at index 0, and selectedCornerstone was updated
        val ccStatus = rsm.exemptCornerstoneCase()

        //Then
        ccStatus shouldBe CornerstoneStatus()
    }

    @Test
    fun `cornerstoneStatus should reflect updated selection after exemptCornerstoneCase`() {
        //Given
        val cc1 = kb.addCornerstoneCase(createCase("Case1"))
        kb.addCornerstoneCase(createCase("Case2"))
        kb.addCornerstoneCase(createCase("Case3"))
        val sessionCase = createCase("Session")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        //When - select Case3 (index 2), exempt it
        rsm.selectCornerstone(2)
        rsm.exemptCornerstoneCase()
        val status = rsm.cornerstoneStatus()

        //Then - Case3 exempted, Case1 and Case2 remain, selected should be Case2 (coerced index)
        status.numberOfCornerstones shouldBe 2
        status.indexOfCornerstoneToReview shouldBe 1
    }

    private fun glucose() = kb.attributeManager.getOrCreate("Glucose")

    private fun createCase(caseName: String, glucoseValue: String = "0.667", id: Long? = null): RDRCase {
        with(RDRCaseBuilder()) {
            addValue(glucose(), defaultDate, glucoseValue)
            return build(caseName, id)
        }
    }

    private fun createKB(kbInfo: KBInfo): KB {
        val newKb = KB(InMemoryKB(kbInfo))
        rsm = KBSession(newKb).ruleSessionManager
        return newKb
    }
}