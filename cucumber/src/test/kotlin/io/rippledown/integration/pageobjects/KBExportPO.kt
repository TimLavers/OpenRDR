package io.rippledown.integration.pageobjects

import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement

class KBExportPO(private val driver: WebDriver) {

    init {
        driver.waitForElementWithId("confirm_zip_export")
    }

    fun doExport() {
        confirmExportButton().click()
    }

    private fun confirmExportButton(): WebElement = driver.waitForElementWithId("confirm_zip_export")
}