package io.rippledown.integration.pageobjects

import io.rippledown.constants.kb.KB_IMPORT_BUTTON_ID
import io.rippledown.integration.utils.Cyborg
import org.awaitility.Awaitility
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import java.io.File
import java.time.Duration


class KBImportPO(private val driver: WebDriver) {

    init {
        driver.waitForElementWithId("select_zip")
    }

    fun selectZipAndDoImport(file: File) {
        val selectFileButton = selectZipInput()
        selectFileButton.sendKeys(file.absolutePath)
        waitForImportButtonToBeEnabled()
        // Directly clicking with Selenium fails, as do
        // Actions, using JS, and calling submit.
        val borg = Cyborg()
        borg.tab()
        borg.tab()
        borg.tab()
        borg.enter()
    }

    private fun selectZipInput(): WebElement = driver.waitForElementWithId("select_zip")

    private fun waitForImportButtonToBeEnabled() {
        Awaitility.await().atMost(Duration.ofSeconds(5)).until {
            importButton().isEnabled
        }
    }

    private fun importButton(): WebElement {
        return driver.waitForElementWithId(KB_IMPORT_BUTTON_ID)
    }
}