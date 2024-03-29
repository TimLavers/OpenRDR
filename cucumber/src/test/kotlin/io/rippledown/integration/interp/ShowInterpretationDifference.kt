package io.rippledown.integration.interp

import io.kotest.matchers.shouldBe
import io.rippledown.integration.UITestBase
import io.rippledown.integration.pageobjects.CaseQueuePO
import io.rippledown.integration.pageobjects.CaseViewPO
import io.rippledown.integration.pageobjects.InterpretationViewPO
import io.rippledown.integration.restclient.RESTClient
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.episodic.predicate.LessThanOrEquals
import io.rippledown.model.condition.episodic.predicate.Normal
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class ShowInterpretationDifference : UITestBase() {

    private lateinit var caseQueuePO: CaseQueuePO
    private lateinit var caseViewPO: CaseViewPO
    private lateinit var interpretationViewPO: InterpretationViewPO
    private val caseName = "Case1"
    private val tshComment = "Your patient has a normal TSH."

    @BeforeTest
    fun setup() {
        serverProxy.start()
        resetKB()
        setupCase()
        buildRuleForTSHComment()
//        setupWebDriver()
//        caseQueuePO = CaseQueuePO(driver).apply { waitForNumberOfCasesToBe(1) }
//        caseViewPO = CaseViewPO(driver)
//        interpretationViewPO = InterpretationViewPO(driver)
    }

    @AfterTest
    fun cleanup() {
        serverProxy.shutdown()
    }

    private fun buildRuleForTSHComment() {
        with(RESTClient()) {
            getCaseWithName(caseName)
            val tsh = getOrCreateAttribute("TSH")
            val conclusion = getOrCreateConclusion(tshComment)
            startSessionToAddConclusionForCurrentCase(conclusion)
            addConditionForCurrentSession(EpisodicCondition(null, tsh, Normal, Current))
            addConditionForCurrentSession(EpisodicCondition(null, tsh, LessThanOrEquals(0.7), Current))
            commitCurrentSession()
        }
    }

    @Test
    fun shouldShowTheChangeWhenASentenceIsAdded() {
        caseViewPO.nameShown() shouldBe caseName
        val addedText = " This is a new sentence."
        interpretationViewPO
            .appendVerifiedText(addedText)
            .waitForInterpretationText("$tshComment$addedText")
            .selectChangesTab()
            .requireOriginalTextInRow(0, tshComment)
            .requireChangedTextInRow(0, tshComment)
            .requireNoCheckBoxInRow(0)
            .requireOriginalTextInRow(1, "")
            .requireChangedTextInRow(1, addedText.trim())
            .requireCheckBoxInRow(1)
    }

    @Test
    fun shouldShowTheChangeWhenASentenceIsRemoved() {
        caseViewPO.nameShown() shouldBe caseName
        interpretationViewPO
            .deleteAllText()
            .waitForInterpretationText("")
            .selectChangesTab()
            .requireOriginalTextInRow(0, tshComment)
            .requireChangedTextInRow(0, "")
            .requireCheckBoxInRow(0)
    }

    @Test
    fun shouldShowTheChangeWhenASentenceIsReplaced() {
        caseViewPO.nameShown() shouldBe caseName
        val replacementText = "This is a new sentence."
        interpretationViewPO
            .deleteAllText()
            .enterVerifiedText(replacementText)
            .waitForInterpretationText(replacementText)
            .selectChangesTab()
            .requireOriginalTextInRow(0, tshComment)
            .requireChangedTextInRow(0, replacementText)
            .requireCheckBoxInRow(0)
    }

    @Test
    fun `should update the change count whenever verified text is entered`() {
        caseViewPO.nameShown() shouldBe caseName
        interpretationViewPO
            .waitForInterpretationText(tshComment)
            .requireNoBadge()
            .enterVerifiedText(" Go to Bondi. Bring your flippers.") //two additions
            .requireBadgeCount(2)
            .deleteAllText()
            .requireBadgeCount(1)
            .enterVerifiedText(tshComment)
            .requireNoBadge() //back to the original
    }

   @Test
    fun `should show one Unchanged sentence if the user has not changed a non-blank interpretation`() {
        caseViewPO.nameShown() shouldBe caseName
        with (interpretationViewPO) {
            waitForInterpretationText(tshComment)
            selectChangesTab()
            requireOriginalTextInRow(0, tshComment)
            requireChangedTextInRow(0, tshComment)
        }
    }

    private fun setupCase() = labProxy.provideCase(caseName)

}