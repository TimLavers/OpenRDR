package io.rippledown.integration

import io.rippledown.CaseTestUtils
import io.rippledown.integration.pageobjects.CaseQueuePO
import org.apache.commons.io.FileUtils
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.io.File
import java.time.Duration
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class CaseQueueTest: UITestBase() {

    @BeforeTest
    fun setup() {
        cleanupCasesDir()
        setupWebDriver()
    }

    @AfterTest
    fun cleanup() {
        driverClose()
    }

    @Test
    fun numberOfWaitingCasesIsShown() {
        val caseQueuesPO = CaseQueuePO(driver)
        // No cases at start.
        assertEquals(caseQueuesPO.numberWaiting(), 0)

        // Copy a case.
        copyCase("Case2")
        caseQueuesPO.refresh()
        assertEquals(caseQueuesPO.numberWaiting(), 1)

        // Copy another case.
        copyCase("Case1")
        caseQueuesPO.refresh()
        assertEquals(caseQueuesPO.numberWaiting(), 2)
    }

    private fun cleanupCasesDir() {
        val destination = File("temp/cases")
        FileUtils.cleanDirectory(destination)
    }

    private fun copyCase(caseName: String) {
        val destination = File("temp/cases")
        FileUtils.copyFileToDirectory(CaseTestUtils.caseFile(caseName), destination)
    }
}