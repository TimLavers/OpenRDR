package io.rippledown.integration

import io.rippledown.integration.pageobjects.CaseListPO
import io.rippledown.integration.pageobjects.CaseQueuePO
import io.rippledown.integration.pageobjects.NoCaseViewPO
import kotlin.test.*

// ORD2
internal class MultiEpisodeCase: UITestBase() {

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
    fun twoEpisodes() {
        val caseViewPO = caseListPO.select("Case4")
        assertEquals(caseViewPO.nameShown(), "Case4")
        val dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown.size, 2)
        assertEquals(dataShown["TSH"]!![0], "0.67 mU/L")
        assertEquals(dataShown["TSH"]!![1], "2.75 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
        assertEquals(dataShown["Stuff"]!![0], "12.4 mU/L")
        assertEquals(dataShown["Stuff"]!![1], "6.7 mU/L")
        assertEquals(caseViewPO.referenceRange("Stuff"), "")

        val datesShown = caseViewPO.datesShown()
        assertEquals(2, datesShown.size)
        assertEquals("2022-08-05 12:31", datesShown[0])
        assertEquals("2022-08-06 02:25", datesShown[1])
    }

    private fun setupCases() {
        labServerProxy.cleanCasesDir()
        labServerProxy.copyCase("Case4")
    }
}