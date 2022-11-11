package io.rippledown.integration

import io.kotest.matchers.shouldBe
import io.rippledown.integration.pageobjects.CaseListPO
import io.rippledown.integration.pageobjects.CaseQueuePO
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class CaseQueueTest: UITestBase() {

    private lateinit var caseQueuePO: CaseQueuePO
    private lateinit var caseListPO: CaseListPO

    @BeforeTest
    fun setup() {
        serverProxy.start()
        setupWebDriver()
        caseQueuePO = CaseQueuePO(driver)
        caseListPO = CaseListPO(driver)

    }

    @AfterTest
    fun cleanup() {
        driverClose()
        serverProxy.shutdown()
    }

    @Test
    fun showCaseList() {
        labProxy.copyCase("Case2")
        labProxy.copyCase("Case1")
        caseQueuePO.waitForNumberWaitingToBe(2)

        val casesListed = caseListPO.casesListed()
        assertEquals(casesListed[0], "Case1")
        assertEquals(casesListed[1], "Case2")
    }

    @Test
    fun numberOfWaitingCasesIsShown() {
        // No cases at start.
        caseQueuePO.numberWaiting() shouldBe 0

        // Copy a case.
        labProxy.copyCase("Case2")
        caseQueuePO.waitForNumberWaitingToBe(1)

        // Copy another case.
        labProxy.copyCase("Case1")
        caseQueuePO.waitForNumberWaitingToBe(2)
    }
}