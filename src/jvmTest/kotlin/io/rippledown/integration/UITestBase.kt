package io.rippledown.integration

import io.rippledown.CaseTestUtils
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
    val inputDir = File("temp/cases")

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
        FileUtils.cleanDirectory(inputDir)
    }

    fun copyCase(caseName: String) {
        FileUtils.copyFileToDirectory(CaseTestUtils.caseFile(caseName), inputDir)
    }

    fun writeCaseToInputDir(rdrCase: RDRCase) {
        val file = File(inputDir, "${rdrCase.name}.json")
        val serialized = Json.encodeToString(rdrCase)
        FileUtils.writeStringToFile(file, serialized, UTF_8)
    }
}