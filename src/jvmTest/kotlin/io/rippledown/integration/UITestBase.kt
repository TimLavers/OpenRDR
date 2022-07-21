package io.rippledown.integration

import io.rippledown.CaseTestUtils
import org.apache.commons.io.FileUtils
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.io.File
import java.time.Duration

open class UITestBase {

    lateinit var driver: WebDriver

    fun setupWebDriver() {
        System.setProperty("webdriver.chrome.driver", "C:\\chromedriver\\chromedriver.exe")
        val options = ChromeOptions()
        driver = ChromeDriver(options)
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10))
        driver.manage().window()?.maximize()
        driver.get("http://127.0.0.1:9090")
    }

    fun driverClose() {
        driver.close()
    }

    fun cleanupCasesDir() {
        val destination = File("temp/cases")
        FileUtils.cleanDirectory(destination)
    }

    fun copyCase(caseName: String) {
        val destination = File("temp/cases")
        FileUtils.copyFileToDirectory(CaseTestUtils.caseFile(caseName), destination)
    }

}