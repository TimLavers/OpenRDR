package io.rippledown.kb

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.rule.ChangeTreeToAddConclusion
import io.rippledown.model.rule.ConditionSuggester
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
    fun `exemptCornerstoneCase should return empty status when conditions have filtered out all cornerstones`() {
        //Given
        val cc1 = kb.addCornerstoneCase(createCase("Case1", "5.0"))
        val cc2 = kb.addCornerstoneCase(createCase("Case2", "6.0"))
        val sessionCase = createCase("Session", "0.667")
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        // Sanity check: initially there are 2 cornerstones
        kb.cornerstoneStatus().numberOfCornerstones shouldBe 2

        // Add a condition that filters out all cornerstones (Glucose is "0.667" only for Session)
        val conditionSuggester = ConditionSuggester(kb.attributeManager.all(), sessionCase)
        val isCondition = conditionSuggester.suggestions().first { it.asText() == "Glucose is \"0.667\"" }
        kb.addConditionToCurrentRuleSession(isCondition.initialSuggestion())

        // Now all cornerstones are filtered out
        kb.cornerstoneStatus().numberOfCornerstones shouldBe 0

        //When - LLM erroneously sends ExemptCornerstone despite no cornerstones
        val ccStatus = kb.exemptCornerstoneCase()

        //Then - should return empty status without crashing
        ccStatus shouldBe CornerstoneStatus()
    }

    @Test
    fun `exemptCornerstone with negative index should return empty status without crashing`() {
        //Given
        kb.addCornerstoneCase(createCase("Case1"))
        kb.addCornerstoneCase(createCase("Case2"))
        val sessionCase = createCase("Session")
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        //When
        val ccStatus = kb.exemptCornerstone(-1)

        //Then
        ccStatus shouldBe CornerstoneStatus()
    }

    @Test
    fun `exemptCornerstone with arbitrary negative index should return empty status`() {
        //Given
        kb.addCornerstoneCase(createCase("Case1"))
        val sessionCase = createCase("Session")
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        //When
        val ccStatus = kb.exemptCornerstone(-42)

        //Then
        ccStatus shouldBe CornerstoneStatus()
    }

    @Test
    fun `exemptCornerstone should return empty status when no cornerstones were ever added`() {
        //Given - no cornerstone cases added at all
        val sessionCase = createCase("Session")
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        //When
        val ccStatus = kb.exemptCornerstone(0)

        //Then
        ccStatus shouldBe CornerstoneStatus()
    }

    @Test
    fun `exemptCornerstoneCase should return empty status when no cornerstones were ever added`() {
        //Given - no cornerstone cases added at all
        val sessionCase = createCase("Session")
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        //When
        val ccStatus = kb.exemptCornerstoneCase()

        //Then
        ccStatus shouldBe CornerstoneStatus()
    }

    @Test
    fun `exemptCornerstone with negative index should clear selected cornerstone`() {
        //Given
        val cc1 = kb.addCornerstoneCase(createCase("Case1"))
        kb.addCornerstoneCase(createCase("Case2"))
        val sessionCase = createCase("Session")
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        // Select a cornerstone first
        kb.selectCornerstone(0)
        kb.cornerstoneStatus().cornerstoneToReview shouldBe kb.viewableCase(cc1)

        //When
        kb.exemptCornerstone(-1)

        //Then - selectedCornerstone should be cleared, so cornerstoneStatus falls back to first
        val status = kb.cornerstoneStatus()
        status.numberOfCornerstones shouldBe 2
        status.indexOfCornerstoneToReview shouldBe 0
    }

    @Test
    fun `exemptCornerstoneCase should return empty status when called repeatedly after conditions filter out all cornerstones`() {
        //Given
        kb.addCornerstoneCase(createCase("Case1", "5.0"))
        kb.addCornerstoneCase(createCase("Case2", "6.0"))
        val sessionCase = createCase("Session", "0.667")
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        // Add a condition that filters out all cornerstones
        val conditionSuggester = ConditionSuggester(kb.attributeManager.all(), sessionCase)
        val isCondition = conditionSuggester.suggestions().first { it.asText() == "Glucose is \"0.667\"" }
        kb.addConditionToCurrentRuleSession(isCondition.initialSuggestion())
        kb.cornerstoneStatus().numberOfCornerstones shouldBe 0

        //When - call exemptCornerstoneCase multiple times
        val ccStatus1 = kb.exemptCornerstoneCase()
        val ccStatus2 = kb.exemptCornerstoneCase()

        //Then - both should return empty status without crashing
        ccStatus1 shouldBe CornerstoneStatus()
        ccStatus2 shouldBe CornerstoneStatus()
    }

    @Test
    fun `exemptCornerstoneCase should work correctly when conditions filter out some but not all cornerstones`() {
        //Given
        kb.addCornerstoneCase(createCase("Case1", "0.667"))  // same as session case
        kb.addCornerstoneCase(createCase("Case2", "5.0"))    // different
        val sessionCase = createCase("Session", "0.667")
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        // Sanity: initially 2 cornerstones
        kb.cornerstoneStatus().numberOfCornerstones shouldBe 2

        // Add a condition that filters out Case2 but keeps Case1
        val conditionSuggester = ConditionSuggester(kb.attributeManager.all(), sessionCase)
        val isCondition = conditionSuggester.suggestions().first { it.asText() == "Glucose is \"0.667\"" }
        kb.addConditionToCurrentRuleSession(isCondition.initialSuggestion())

        // Now only Case1 remains
        kb.cornerstoneStatus().numberOfCornerstones shouldBe 1

        //When - exempt the remaining cornerstone
        val ccStatus = kb.exemptCornerstoneCase()

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