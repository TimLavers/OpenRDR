package io.rippledown.integration

import io.rippledown.integration.pageobjects.CaseListPO
import io.rippledown.integration.pageobjects.CaseQueuePO
import kotlin.test.*

// ORD2
internal class SendInterpretationTest: UITestBase() {

    private lateinit var caseQueuePO: CaseQueuePO
    private lateinit var caseListPO: CaseListPO

    @BeforeTest
    fun setup() {
        setupCases()
        setupWebDriver()
        caseQueuePO = CaseQueuePO(driver)
        pause()//todo use Awaitility
        caseQueuePO.refresh()
        caseListPO = caseQueuePO.review()
    }

    @AfterTest
    fun cleanup() {
        driverClose()
    }

    @Test
    fun setInterpretationForCase() {
        // Initially, no interpretations have been sent to the lab system.
        assertEquals(labServerProxy.interpretationsReceived(), 0)

        // Check that Case1, Case2 and Case3 are listed.
        assertEquals(caseListPO.casesListed(), listOf("Case1", "Case2", "Case3"))

        // Select Case2.
        val caseViewPO = caseListPO.select("Case2")
        assertEquals(caseViewPO.nameShown(), "Case2") //sanity

        // Set its interpretation and send it.
        val case2Interpretation = "Puzzling results."
        caseViewPO.setInterpretationText(case2Interpretation)
        pause()

        // Check that 1 interpretation has now been received by the lab system.
        assertEquals(labServerProxy.interpretationsReceived(), 1)

        // Check that the interpretation for Case2 is as set in the user interface.
        assertEquals(labServerProxy.interpretationReceived("Case2"), case2Interpretation)

        // Check that there are two remaining input cases: Case1 and Case3.
        assertEquals(labServerProxy.inputCases(), setOf("Case1", "Case3"))

        // Check that the case list in the user interface shows just Case1 and Case3.
        assertEquals(caseListPO.casesListed(), listOf("Case1", "Case3"))

        // Check that Case1 is selected.
        assertEquals(caseViewPO.nameShown(), "Case1") //sanity

        // Check that the interpretation field is blank.
        assertEquals(caseViewPO.interpretationText(), "")
    }

    private fun setupCases() {
        labServerProxy.cleanInterpretationsDir()
        labServerProxy.cleanCasesDir()
        labServerProxy.copyCase("Case1")
        labServerProxy.copyCase("Case2")
        labServerProxy.copyCase("Case3")
    }
}