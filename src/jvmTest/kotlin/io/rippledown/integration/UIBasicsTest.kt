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

internal class UIBasicsTest {

    private lateinit var driver: WebDriver

    @BeforeTest
    fun setup() {
        System.setProperty("webdriver.chrome.driver","C:\\chromedriver\\chromedriver.exe")
        val options = ChromeOptions()
        driver = ChromeDriver(options)
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10))
        driver.manage().window()?.maximize()
        driver.get("http://127.0.0.1:9090")
    }

    @AfterTest
    fun driverClose() {
        driver.close()
    }

    @Test
    fun basicElements() {
        val headingElement = driver.findElement(By.id("main_heading"))
        assertEquals(headingElement.text, "Open RippleDown")
    }
}