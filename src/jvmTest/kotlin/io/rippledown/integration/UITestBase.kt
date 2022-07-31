package io.rippledown.integration

import io.rippledown.CaseTestUtils
import io.rippledown.integration.labsystem.LabServerProxy
import io.rippledown.model.RDRCase
import org.apache.commons.io.FileUtils
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.io.File
import java.time.Duration
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.UTF_8

open class UITestBase {
    val labServerProxy = LabServerProxy()
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

}