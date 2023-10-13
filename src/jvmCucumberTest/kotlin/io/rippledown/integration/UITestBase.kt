package io.rippledown.integration

import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
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
    val restClient = RESTClient()
    val attributeFactory = io.rippledown.integration.RestClientAttributeFactory(restClient)
    val conclusionFactory = io.rippledown.integration.RestClientConclusionFactory(restClient)
    val conditionFactory = io.rippledown.integration.RestClientConditionFactory(restClient)
    val labProxy = LabProxy(serverProxy.tempDir(), restClient)
    private val dirProxy = DirProxy()
    lateinit var driver: WebDriver

    lateinit var playwrightPage: Page

    fun setupWebDriver(): WebDriver {
        driver = getChromeDriver()
        with(driver) {
            manage().timeouts().implicitlyWait(Duration.ofSeconds(10))
            manage().window()?.maximize()
            get("http://localhost:9090")
        }
        return driver
    }


    fun setupPlaywright(): Page {
        val playwright = Playwright.create()
        val browser = playwright.chromium().launch(
            BrowserType.LaunchOptions()
                .setHeadless(false)
        )
        playwrightPage = browser.newPage()
        playwrightPage.navigate("http://localhost:9090")
        playwright.selectors().setTestIdAttribute("id");


//        browser.close()
//        playwright.close()
        return playwrightPage

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
        val tabs = driver.windowHandles as LinkedHashSet<String>
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