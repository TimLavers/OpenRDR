package io.rippledown.integration

import io.kotest.matchers.shouldBe
import io.rippledown.integration.pageobjects.CaseListPO
import io.rippledown.integration.pageobjects.CaseQueuePO
import io.rippledown.integration.pageobjects.InterpretationViewPO
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

// ORD2
internal class SaveInterpretationTest : io.rippledown.integration.UITestBase() {

    private lateinit var caseQueuePO: CaseQueuePO
    private lateinit var caseListPO: CaseListPO
    private lateinit var interpretationViewPO: InterpretationViewPO

    @BeforeTest
    fun setup() {
        serverProxy.start()
        setupCases()
        setupWebDriver()
        caseQueuePO = CaseQueuePO(driver)
        caseQueuePO.waitForNumberOfCasesToBe(3)
        caseListPO = CaseListPO(driver)
        interpretationViewPO = InterpretationViewPO(driver)
    }

    @AfterTest
    fun cleanup() {
        driverClose()
        serverProxy.shutdown()
    }

//    @Test
    fun setInterpretationForCase() {
        // Check that Case1, Case2 and Case3 are listed.
        caseListPO.casesListed() shouldBe listOf("Case1", "Case2", "Case3")

        // Select Case2.
        val caseViewPO = caseListPO.select("Case2")
        caseViewPO.nameShown() shouldBe "Case2" //sanity

        // Set its interpretation.
        val verifiedReport = "Puzzling results."
        interpretationViewPO.enterVerifiedText(verifiedReport)

        // Check that the case list in the user interface still shows just Case1, Case2 and Case3.
        caseListPO.waitForCaseListToHaveSize(3)
        caseListPO.casesListed() shouldBe listOf("Case1", "Case2", "Case3")

        caseListPO.select("Case1")
        interpretationViewPO.interpretationText() shouldBe ""

        // Reselect Case2
        caseListPO.select("Case2")
        caseViewPO.nameShown() shouldBe "Case2" //sanity

        // Check that its verified text has been saved.
        interpretationViewPO.interpretationText() shouldBe verifiedReport
    }

    private fun setupCases() {
        labProxy.provideCase("Case1")
        labProxy.provideCase("Case2")
        labProxy.provideCase("Case3")
    }
}