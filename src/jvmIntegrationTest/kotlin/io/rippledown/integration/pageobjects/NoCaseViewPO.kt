package io.rippledown.integration.pageobjects

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver

class NoCaseViewPO(private val driver: WebDriver) {

    fun text(): String {
        return driver.findElement(By.id("no_case_view")).text
    }
}