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
import java.time.Duration.ofSeconds


open class UITestBase {
    val serverProxy = ServerProxy()
    val restClient = RESTClient()
    val attributeFactory = RestClientAttributeFactory(restClient)
    val conclusionFactory = RestClientConclusionFactory(restClient)
    val conditionFactory = RestClientConditionFactory(restClient)
    val labProxy = LabProxy(serverProxy.tempDir(), restClient)
    private val dirProxy = DirProxy()
    lateinit var driver: WebDriver

    fun setupWebDriver(): WebDriver {
        driver = getChromeDriver()
        with(driver) {
            manage().timeouts().implicitlyWait(ofSeconds(10))
            manage().window()?.maximize()
            get("http://localhost:9090")
        }
        return driver
    }

    fun downloadsDir() = dirProxy.downloadsDir()

    private fun getChromeDriver(): WebDriver {
        ChromeDriverManager.getInstance(CHROME)
            .clearResolutionCache()
            .setup()
        val options = ChromeOptions()
        with(options) {
            addArguments("--remote-allow-origins=*")
            addArguments("--disable-extensions")
            addArguments("--disable-application-cache")
            addArguments("--disable-web-security")
            addArguments("--remote-allow-origins=*")
            val prefsMap = mutableMapOf<String, Any>()
            prefsMap["download.default_directory"] = downloadsDir().absolutePath
            options.setExperimentalOption("prefs", prefsMap)
        }
        return ChromeDriver(options)
    }

    fun driverClose() {
        driver.quit()
    }

    fun resetKB() {
        restClient.resetKB()
    }
}