package io.rippledown.integration

import io.github.bonigarcia.wdm.config.DriverManagerType.CHROME
import io.github.bonigarcia.wdm.managers.ChromeDriverManager
import io.rippledown.integration.proxy.LabProxy
import io.rippledown.integration.proxy.ServerProxy
import io.rippledown.integration.restclient.RESTClient
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.time.Duration

open class UITestBase {
    val serverProxy = ServerProxy()
    val labProxy = LabProxy(serverProxy.tempDir())
    val restClient = RESTClient()
    lateinit var driver: WebDriver

    fun setupWebDriver(): WebDriver {
        driver = getChromeDriver()
        with(driver) {
            manage().timeouts().implicitlyWait(Duration.ofSeconds(10))
            manage().window()?.maximize()
            get("http://localhost:9090")
        }
        return driver
    }

    private fun getChromeDriver(): WebDriver {
        ChromeDriverManager.getInstance(CHROME).setup()
        val options = ChromeOptions()
        with(options) {
            addArguments("--disable-extensions")
            addArguments("--disable-application-cache")
            addArguments("--disable-web-security")
        }
        return ChromeDriver(options)
    }

    fun driverClose() {
        val tabs = driver.getWindowHandles() as LinkedHashSet<String>
        tabs.forEach {
            driver.switchTo().window(it)
            driver.close()
        }
        driver.quit()
    }

    fun resetKB() {
        restClient.resetKB()
    }
}