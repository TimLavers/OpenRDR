package io.rippledown.integration

import io.kotest.matchers.shouldBe
import io.rippledown.integration.pageobjects.CaseQueuePO
import kotlin.test.*

internal class CaseQueueTest: UITestBase() {

    private lateinit var caseQueuePO: CaseQueuePO

    @BeforeTest
    fun setup() {
        serverProxy.start()
        setupWebDriver()
        caseQueuePO = CaseQueuePO(driver)
    }

    @AfterTest
    fun cleanup() {
        driverClose()
        serverProxy.shutdown()
    }

    @Test
    fun reviewButtonDisabledIfNoCasesWaiting() {
        assertFalse(caseQueuePO.reviewButtonIsEnabled())
    }

    @Test
    fun reviewButtonEnabledIfCasesWaiting() {
        labProxy.copyCase("Case2")
        caseQueuePO.refresh().waitForNumberWaitingToBe(1)
        assertTrue(caseQueuePO.reviewButtonIsEnabled())
    }

    @Test
    fun showCaseList() {
        labProxy.copyCase("Case2")
        labProxy.copyCase("Case1")
        caseQueuePO.refresh().waitForNumberWaitingToBe(2)

        val listPO = caseQueuePO.review()
        val casesListed = listPO.casesListed()
        assertEquals(casesListed[0], "Case1")
        assertEquals(casesListed[1], "Case2")
    }

    @Test
    fun numberOfWaitingCasesIsShown() {
        // No cases at start.
        caseQueuePO.numberWaiting() shouldBe 0

        // Copy a case.
        labProxy.copyCase("Case2")
        caseQueuePO.refresh().waitForNumberWaitingToBe(1)

        // Copy another case.
        labProxy.copyCase("Case1")
        caseQueuePO.refresh().waitForNumberWaitingToBe(2)
    }
}