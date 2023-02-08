package io.rippledown.integration

import io.github.bonigarcia.wdm.config.DriverManagerType.CHROME
import io.github.bonigarcia.wdm.managers.ChromeDriverManager
import io.rippledown.integration.proxy.DirProxy
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
    private val restClient = RESTClient()
    private val dirProxy = DirProxy()
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

    fun downloadsDir() = dirProxy.downloadsDir()

    private fun getChromeDriver(): WebDriver {
        ChromeDriverManager.getInstance(CHROME).setup()
        val options = ChromeOptions()
        with(options) {
            addArguments("--disable-extensions")
            addArguments("--disable-application-cache")
            addArguments("--disable-web-security")
            val prefsMap = mutableMapOf<String, Any>()
            prefsMap.put("download.default_directory", downloadsDir().absolutePath)
            options.setExperimentalOption("prefs", prefsMap )
        }
        return ChromeDriver(options)
    }

    fun driverClose() {
        val tabs = driver.getWindowHandles() as LinkedHashSet<String>
        tabs.forEach {
            driver.switchTo().window(it)
            driver.close()
        }
    }

    fun resetKB() {
        restClient.resetKB()
    }
}