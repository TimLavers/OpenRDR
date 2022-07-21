package io.rippledown.integration

import io.rippledown.integration.pageobjects.CaseQueuePO
import kotlin.test.*

internal class CaseQueueTest: UITestBase() {

    private lateinit var caseQueuePO: CaseQueuePO

    @BeforeTest
    fun setup() {
        cleanupCasesDir()
        setupWebDriver()
        caseQueuePO = CaseQueuePO(driver)
    }

    @AfterTest
    fun cleanup() {
        driverClose()
    }

    @Test
    fun reviewButtonDisabledIfNoCasesWaiting() {
        assertFalse(caseQueuePO.reviewButtonIsEnabled())

        copyCase("Case2")
        pause()//todo use Awaitility
        caseQueuePO.refresh()
        assertTrue(caseQueuePO.reviewButtonIsEnabled())
    }

    @Test
    fun showCaseList() {
        copyCase("Case2")
        copyCase("Case1")
        caseQueuePO.refresh()

        val listPO = caseQueuePO.review()
        val casesListed = listPO.casesListed()
        assertEquals(casesListed.size, 2)
        assertEquals(casesListed[0], "Case1")
        assertEquals(casesListed[1], "Case2")
    }

    @Test
    fun numberOfWaitingCasesIsShown() {
        // No cases at start.
        assertEquals(caseQueuePO.numberWaiting(), 0)

        // Copy a case.
        copyCase("Case2")
        caseQueuePO.refresh()
        assertEquals(caseQueuePO.numberWaiting(), 1)

        // Copy another case.
        copyCase("Case1")
        caseQueuePO.refresh()
        assertEquals(caseQueuePO.numberWaiting(), 2)
    }
}