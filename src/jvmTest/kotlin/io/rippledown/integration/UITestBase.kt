package io.rippledown.integration

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.time.Duration
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

open class UITestBase {

    lateinit var driver: WebDriver

    fun setupWebDriver() {
        System.setProperty("webdriver.chrome.driver","C:\\chromedriver\\chromedriver.exe")
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