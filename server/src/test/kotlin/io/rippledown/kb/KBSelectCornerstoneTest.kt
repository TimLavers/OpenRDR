package io.rippledown.kb

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.utils.defaultDate
import kotlin.test.BeforeTest
import kotlin.test.Test

class KBSelectCornerstoneTest {
    private lateinit var kb: KB
    private lateinit var rsm: RuleSessionManager

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
        rsm.startRuleSessionToAddComment(sessionCase, "Go to Bondi.")

        //When
        val ccStatus = rsm.selectCornerstoneCase(0)

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
        rsm.startRuleSessionToAddComment(sessionCase, "Go to Bondi.")

        //When
        val ccStatus = rsm.selectCornerstoneCase(1)

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
        rsm.startRuleSessionToAddComment(sessionCase, "Go to Bondi.")

        //When
        val ccStatus = rsm.selectCornerstoneCase(2)

        //Then
        ccStatus shouldBe CornerstoneStatus(vcc3, 2, 3)
    }

    @Test
    fun `a selected cornerstone stays selected even when comment rules give it derived values`() {
        //Given three cornerstones that are all given a comment, so their viewable
        //copies carry materialised comment-attribute values that the raw
        //cornerstone cases do not have
        val cc1 = kb.addProcessedCase(createCase("Case1", glucoseValue = "1.0"))
        rsm.startRuleSessionToAddComment(kb.viewableCase(cc1), "Comment 1.")
        rsm.commitCurrentRuleSession()
        kb.addCornerstoneCase(createCase("Case2", glucoseValue = "2.0"))
        kb.addCornerstoneCase(createCase("Case3", glucoseValue = "3.0"))
        val sessionCase = createCase("Session")
        rsm.startRuleSessionToAddComment(sessionCase, "Go to Bondi.")

        //When the second cornerstone is selected
        val selected = rsm.selectCornerstoneCase(1)

        //Then the selection is retained by the current cornerstone status
        selected.indexOfCornerstoneToReview shouldBe 1
        rsm.cornerstoneStatus().indexOfCornerstoneToReview shouldBe 1
    }

    @Test
    fun `cornerstoneStatus matches the current cornerstone by case id, not by case data`() {
        //Given a comment rule that gives every case a comment-attribute value
        val cc1 = kb.addProcessedCase(createCase("Case1", glucoseValue = "1.0"))
        rsm.startRuleSessionToAddComment(kb.viewableCase(cc1), "Comment 1.")
        rsm.commitCurrentRuleSession()
        kb.addCornerstoneCase(createCase("Case2", glucoseValue = "2.0"))
        val cc3 = kb.addCornerstoneCase(createCase("Case3", glucoseValue = "3.0"))
        val sessionCase = createCase("Session")
        rsm.startRuleSessionToAddComment(sessionCase, "Go to Bondi.")

        //When the status is asked for the viewable copy of the last cornerstone,
        //whose materialised comment value makes its data differ from the raw case
        val viewableCopy = kb.viewableCase(cc3)
        withClue("sanity check: the viewable copy has materialised derived values") {
            viewableCopy.case.hasSameDataAs(cc3) shouldBe false
        }
        val status = rsm.cornerstoneStatus(viewableCopy)

        //Then the status points at that cornerstone, not the first one
        status.indexOfCornerstoneToReview shouldBe 2
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
