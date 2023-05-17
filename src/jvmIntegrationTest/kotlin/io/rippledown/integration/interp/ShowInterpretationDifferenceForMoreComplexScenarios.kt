package io.rippledown.integration.interp

import io.kotest.matchers.shouldBe
import io.rippledown.integration.UITestBase
import io.rippledown.integration.pageobjects.CaseListPO
import io.rippledown.integration.pageobjects.CaseQueuePO
import io.rippledown.integration.pageobjects.CaseViewPO
import io.rippledown.integration.pageobjects.InterpretationViewPO
import io.rippledown.integration.restclient.RESTClient
import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import io.rippledown.model.condition.IsNormal
import io.rippledown.model.condition.LessThanOrEqualTo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test


internal class ShowInterpretationDifferenceForMoreComplexScenarios : UITestBase() {

    private lateinit var caseQueuePO: CaseQueuePO
    private lateinit var caseListPO: CaseListPO
    private lateinit var caseViewPO: CaseViewPO
    private lateinit var interpretationViewPO: InterpretationViewPO
    private val tshComment = "Your patient has a normal TSH."
    private val ft4Comment = "Your patient has an abnormal FT4."
    private val tsh = Attribute("TSH")
    private val condition1 = IsNormal(tsh)
    private val condition2 = LessThanOrEqualTo(tsh, 0.7)

    @BeforeTest
    fun setup() {
        serverProxy.start()
        resetKB()
        setupWebDriver()
        caseQueuePO = CaseQueuePO(driver)
        caseListPO = CaseListPO(driver)
        caseViewPO = CaseViewPO(driver)
        interpretationViewPO = InterpretationViewPO(driver)
    }

    @AfterTest
    fun cleanup() {
        driverClose()
        serverProxy.shutdown()
    }

    private fun buildRuleForSentence(caseName: String, sentence: String) {
        with(RESTClient()) {
            getCaseWithName(caseName)
            startSessionToAddConclusionForCurrentCase(Conclusion(sentence))
            addConditionForCurrentSession(condition1)
            addConditionForCurrentSession(condition2)
            commitCurrentSession()
        }
    }

    @Test
    fun `should show no changes if the user has not changed an empty interpretation`() {
        val caseName = "case1"
        labProxy.writeNewCaseFile(caseName)
        caseQueuePO.waitForNumberOfCasesToBe(1)
        caseViewPO.nameShown() shouldBe caseName
        interpretationViewPO
            .requireChangesLabel("CHANGES")
            .requireInterpretationText("")
            .selectChangesTab()
            .requireNoRowsInDiffTable()
    }

}