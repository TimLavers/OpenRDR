package io.rippledown.integration

import io.rippledown.integration.pageobjects.CaseListPO
import io.rippledown.integration.pageobjects.CaseQueuePO
import io.rippledown.integration.pageobjects.NoCaseViewPO
import kotlin.test.*

// ORD2
internal class CaseViewTest: UITestBase() {

    private lateinit var caseQueuePO: CaseQueuePO
    private lateinit var caseListPO: CaseListPO

    @BeforeTest
    fun setup() {
        resetKB()
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
    fun noCaseSelected() {
        val noCaseView = NoCaseViewPO(driver)
        assertEquals(noCaseView.text(), "No case selected")
    }

    @Test
    fun selectCase() {
        val caseViewPO = caseListPO.select("Case2")
        assertEquals(caseViewPO.nameShown(), "Case2")
        val dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown.size, 2)
        assertEquals(dataShown["TSH"]!![0], "0.72 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
        assertEquals(dataShown["CDE"]!![0], "9.7")
        assertEquals(caseViewPO.referenceRange("CDE"), "")
    }

    private fun setupCases() {
        labServerProxy.cleanCasesDir()
        labServerProxy.copyCase("Case1")
        labServerProxy.copyCase("Case2")
        labServerProxy.copyCase("Case3")
    }
}