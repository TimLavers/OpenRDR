package io.rippledown.integration

import io.rippledown.CaseTestUtils
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
        copyCases()
        setupWebDriver()
    }

    @AfterTest
    fun cleanup() {
        driverClose()
    }

    @Test
    fun numberOfWaitingCasesIsShown() {
        val waitingCasesElement = driver.findElement(By.id("number_of_cases_waiting_value"))
        assertEquals(waitingCasesElement.text, "3")
    }

    private fun copyCases() {
        val destination = File("temp/cases")
        FileUtils.cleanDirectory(destination)
        FileUtils.copyFileToDirectory(CaseTestUtils.caseFile("Case1"), destination)
//        FileUtils.copyFileToDirectory(CaseTestUtils.caseFile("Case2"), destination)
        FileUtils.copyFileToDirectory(CaseTestUtils.caseFile("Case3"), destination)
    }
}