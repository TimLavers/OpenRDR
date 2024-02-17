package io.rippledown.integration.interp

import io.kotest.matchers.shouldBe
import io.rippledown.integration.UITestBase
import io.rippledown.integration.pageobjects.CaseListPO
import io.rippledown.integration.pageobjects.CaseQueuePO
import io.rippledown.integration.pageobjects.CaseViewPO
import io.rippledown.integration.pageobjects.InterpretationViewPO
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test


internal class ShowInterpretationDifferenceForMoreComplexScenarios : UITestBase() {

    private lateinit var caseQueuePO: CaseQueuePO
    private lateinit var caseListPO: CaseListPO
    private lateinit var caseViewPO: CaseViewPO
    private lateinit var interpretationViewPO: InterpretationViewPO

    @BeforeTest
    fun setup() {
        serverProxy.start()
        resetKB()
//        setupWebDriver()
//        caseQueuePO = CaseQueuePO(driver)
//        caseListPO = CaseListPO(driver)
//        caseViewPO = CaseViewPO(driver)
//        interpretationViewPO = InterpretationViewPO(driver)
    }

    @AfterTest
    fun cleanup() {
        serverProxy.shutdown()
    }

    @Test
    fun `should show no changes if the user has not changed an empty interpretation`() {
        val caseName = "case1"
        labProxy.provideCaseWithName(caseName)
        caseQueuePO.waitForNumberOfCasesToBe(1)
        caseViewPO.nameShown() shouldBe caseName
        interpretationViewPO
            .requireChangesLabel("CHANGES")
            .requireInterpretationText("")
            .selectChangesTab()
            .requireNoRowsInDiffTable()
    }
}