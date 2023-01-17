package io.rippledown.integration.pageobjects

import io.rippledown.integration.utils.Cyborg
import org.awaitility.Awaitility
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import java.io.File
import java.time.Duration


class KBImportPO(private val driver: WebDriver) {

    init {
        waitForElementWithId("select_zip")
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

    private fun selectZipInput(): WebElement = waitForElementWithId("select_zip")

    private fun waitForImportButtonToBeEnabled() {
        Awaitility.await().atMost(Duration.ofSeconds(5)).until {
            importButton().isEnabled
        }
    }

    private fun importButton(): WebElement {
        return waitForElementWithId("import_from_zip")
    }

    private fun waitForElementWithId(id: String): WebElement {
        Awaitility.await().atMost(Duration.ofSeconds(3)).until {
            driver.findElement(By.id(id)) != null
        }
        return driver.findElement(By.id(id))
    }
}