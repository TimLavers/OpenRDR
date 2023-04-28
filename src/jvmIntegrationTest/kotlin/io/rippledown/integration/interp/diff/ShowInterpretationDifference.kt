package io.rippledown.integration.interp.diff

import io.kotest.matchers.shouldBe
import io.rippledown.integration.UITestBase
import io.rippledown.integration.pageobjects.CaseQueuePO
import io.rippledown.integration.pageobjects.CaseViewPO
import io.rippledown.integration.pageobjects.InterpretationViewPO
import io.rippledown.integration.restclient.RESTClient
import io.rippledown.integration.stop
import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import io.rippledown.model.condition.IsNormal
import io.rippledown.model.condition.LessThanOrEqualTo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test


internal class ShowInterpretationDifference : UITestBase() {

    private lateinit var caseQueuePO: CaseQueuePO
    private lateinit var caseViewPO: CaseViewPO
    private lateinit var interpretationViewPO: InterpretationViewPO
    private val caseName = "Case1"
    private val tshComment = "Your patient has a normal TSH."
    private val tsh = Attribute("TSH")
    private val condition1 = IsNormal(tsh)
    private val condition2 = LessThanOrEqualTo(tsh, 0.7)

    @BeforeTest
    fun setup() {
        serverProxy.start()
        resetKB()
        setupCase()
        buildRuleForTSH()
        setupWebDriver()
        caseQueuePO = CaseQueuePO(driver).apply { waitForNumberWaitingToBe(1) }
        caseViewPO = CaseViewPO(driver)
        interpretationViewPO = InterpretationViewPO(driver)
    }

    @AfterTest
    fun cleanup() {
        driverClose()
        serverProxy.shutdown()
    }

    private fun buildRuleForTSH() {
        with(RESTClient()) {
            getCaseWithName(caseName)
            startSessionToAddConclusionForCurrentCase(Conclusion(tshComment))
            addConditionForCurrentSession(condition1)
            addConditionForCurrentSession(condition2)
            commitCurrentSession()
        }
    }

    @Test
    fun shouldShowNoChangesIfTheUserHasNotChangedTheInterpretation() {
        caseViewPO.nameShown() shouldBe caseName
        interpretationViewPO
            .requireInterpretationText(tshComment)
            .selectChangesTab()
            .requireOriginalTextInRow(0, tshComment)
            .requireChangedTextInRow(0, tshComment)
        stop()
    }

    @Test
    fun shouldShowTheChangeWhenASentenceIsAdded() {
        caseViewPO.nameShown() shouldBe caseName
        val addedText = " This is a new sentence."
        interpretationViewPO
            .enterVerifiedText(addedText)
            .requireInterpretationText("$tshComment$addedText")
        stop()
        /* .selectChangesTab()
         .requireOriginalTextInRow(0, tshComment)
         .requireChangedTextInRow(0, tshComment)
         .requireNoCheckBoxInRow(0)
         .requireOriginalTextInRow(1, "")
         .requireChangedTextInRow(1, addedText.trim())
         .requireCheckBoxInRow(1)*/
    }

    @Test
    fun shouldShowTheChangeWhenASentenceIsRemoved() {
        caseViewPO.nameShown() shouldBe caseName
        interpretationViewPO
            .deleteAllText()
            .requireInterpretationText("")
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
            .requireInterpretationText(replacementText)
            .selectChangesTab()
            .requireOriginalTextInRow(0, tshComment)
            .requireChangedTextInRow(0, replacementText)
            .requireCheckBoxInRow(0)
    }

    private fun setupCase() {
        labProxy.copyCase(caseName)
    }
}