package io.rippledown.integration

import io.rippledown.integration.pageobjects.CaseListPO
import io.rippledown.integration.pageobjects.CaseQueuePO
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

// ORD2
internal class CaseViewTest : UITestBase() {

    private lateinit var caseQueuePO: CaseQueuePO
    private lateinit var caseListPO: CaseListPO

    @BeforeTest
    fun setup() {
        serverProxy.start()
        setupCases()
//        setupWebDriver()
        caseQueuePO = CaseQueuePO()
        caseQueuePO.waitForNumberOfCasesToBe(3)
//        caseListPO = CaseListPO()
    }

    @AfterTest
    fun cleanup() {
        serverProxy.shutdown()
    }

    @Test
    fun selectCase() {
        val caseViewPO = caseListPO.select("Case2")
        val caseName = caseViewPO.nameShown()!!
        assertEquals(caseName, "Case2")
        val dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown.size, 2)
        assertEquals(dataShown["TSH"]!![0], "0.72 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "0.50 - 4.0")
        assertEquals(dataShown["CDE"]!![0], "9.7")
        assertEquals(caseViewPO.referenceRange("CDE"), "")
    }

    private fun setupCases() {
        labProxy.provideCase("Case1")
        labProxy.provideCase("Case2")
        labProxy.provideCase("Case3")
    }
}