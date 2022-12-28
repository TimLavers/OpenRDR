package io.rippledown.integration.pageobjects

import org.awaitility.Awaitility.await
import org.awaitility.kotlin.withPollInterval
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import java.time.Duration.ofSeconds
import java.util.concurrent.TimeUnit

class KBInfoPO(private val driver: WebDriver) {

    fun headingText(): String? {
        return headingElement().text!!
    }

    private fun headingElement() = driver.findElement(By.id("kb_info_heading"))
}