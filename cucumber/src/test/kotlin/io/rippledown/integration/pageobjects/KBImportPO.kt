package io.rippledown.integration.pageobjects

import io.rippledown.constants.kb.CONFIRM_IMPORT_BUTTON_ID
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import java.io.File


class KBImportPO(private val driver: WebDriver) {

    init {
        driver.waitForElementWithId("select_zip")
    }

    fun selectZipAndDoImport(file: File) {
        val selectFileButton = selectZipInput()
        selectFileButton.sendKeys(file.absolutePath)
        confirmImportButton().click()
    }

    private fun selectZipInput(): WebElement = driver.waitForElementWithId("select_zip")

    private fun confirmImportButton(): WebElement {
        return driver.waitForElementWithId(CONFIRM_IMPORT_BUTTON_ID)
    }
}