package io.rippledown.integration

import io.kotest.matchers.shouldBe
import io.rippledown.integration.pageobjects.CaseListPO
import io.rippledown.integration.pageobjects.CaseQueuePO
import io.rippledown.integration.pageobjects.InterpretationViewPO
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

// ORD2
internal class SendInterpretationTest: UITestBase() {

    private lateinit var caseQueuePO: CaseQueuePO
    private lateinit var caseListPO: CaseListPO
    private lateinit var interpretationViewPO: InterpretationViewPO

    @BeforeTest
    fun setup() {
        serverProxy.start()
        setupCases()
        setupWebDriver()
        caseQueuePO = CaseQueuePO(driver)
        caseQueuePO.waitForNumberWaitingToBe(3)
        caseListPO = CaseListPO(driver)
        interpretationViewPO = InterpretationViewPO(driver)
    }

    @AfterTest
    fun cleanup() {
        driverClose()
        serverProxy.shutdown()
    }

    @Test
    fun setInterpretationForCase() {
        // Initially, no interpretations have been sent to the lab system.
        labProxy.interpretationsReceived() shouldBe 0

        // Check that Case1, Case2 and Case3 are listed.
        caseListPO.casesListed() shouldBe listOf("Case1", "Case2", "Case3")

        // Select Case2.
        val caseViewPO = caseListPO.select("Case2")
        caseViewPO.nameShown() shouldBe "Case2" //sanity

        // Set its interpretation and send it.
        val verifiedReport = "Puzzling results."
        interpretationViewPO.enterVerifiedText(verifiedReport)

        // Check that 1 interpretation has now been received by the lab system.
        labProxy.waitForNumberOfInterpretationsToBe(1)

        // Check that the interpretation for Case2 is as set in the user interface.
        labProxy.interpretationReceived("Case2") shouldBe verifiedReport

        // Check that the case list in the user interface still shows just Case1, Case2 and Case3.
        caseListPO.waitForCaseListToHaveSize(3)
        caseListPO.casesListed() shouldBe listOf("Case1", "Case2", "Case3")

        // Check that Case2 is still selected.
        caseViewPO.nameShown() shouldBe "Case2" //sanity

        // Check that the interpretation field still shows the verified text.
        interpretationViewPO.interpretationText() shouldBe verifiedReport
    }

    private fun setupCases() {
        labProxy.copyCase("Case1")
        labProxy.copyCase("Case2")
        labProxy.copyCase("Case3")
    }
}