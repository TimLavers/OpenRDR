package io.rippledown.integration

import io.rippledown.integration.labsystem.LabServerProxy
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.time.Duration

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