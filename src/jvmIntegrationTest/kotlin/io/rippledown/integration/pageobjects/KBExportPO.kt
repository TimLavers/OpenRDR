package io.rippledown.integration.pageobjects

import io.rippledown.integration.utils.Cyborg
import org.awaitility.Awaitility
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import java.io.File
import java.time.Duration

class KBExportPO(private val driver: WebDriver) {

    init {
        driver.waitForElementWithId("confirm_zip_export")
    }

    fun doExport() {
        confirmExportButton().click()
    }

    private fun confirmExportButton(): WebElement = driver.waitForElementWithId("confirm_zip_export")
}