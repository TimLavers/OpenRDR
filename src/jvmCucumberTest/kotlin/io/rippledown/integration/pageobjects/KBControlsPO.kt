package io.rippledown.integration.pageobjects

import io.rippledown.constants.kb.KB_EXPORT_BUTTON_ID
import io.rippledown.constants.kb.KB_IMPORT_BUTTON_ID
import io.rippledown.constants.kb.KB_SELECTOR_ID
import io.rippledown.integration.proxy.ConfiguredTestData
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.Wait
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.File
import java.time.Duration


class KBControlsPO(private val driver: WebDriver) {

    fun headingText(): String {
        return headingElement().text!!
    }

    private fun kbName(): String {
        return headingText().trim()
    }

    fun importKB(exportedFileName: String) {
        val zipFile = ConfiguredTestData.kbZipFile(exportedFileName)
        importFromZip(zipFile)
    }

    fun importFromZip(zipFile: File) {
        activateImportKB().selectZipAndDoImport(zipFile)
    }

    fun exportKB() {
        headingElement().click()
        val exportButton = driver.findElement(By.id(KB_EXPORT_BUTTON_ID))
        exportButton.click()
        KBExportPO(driver).doExport()
    }

    private fun activateImportKB(): KBImportPO {
        headingElement().click()
        val importButton = driver.findElement(By.id(KB_IMPORT_BUTTON_ID))
        importButton.click()
        return KBImportPO(driver)
    }

    fun waitForKBToBeLoaded(name: String) {
        val wait: Wait<WebDriver> = WebDriverWait(driver, Duration.ofSeconds(10))
        wait.until { _ ->
            kbName() == name
        }
    }

    fun requireKbControlsToBeDisabled() {
        val wait: Wait<WebDriver> = WebDriverWait(driver, Duration.ofSeconds(2))
        wait.until { _ ->
            headingElement().getAttribute("aria-disabled") == "true"
        }
    }

    fun requireKbControlsToBeEnabled() {
        val wait: Wait<WebDriver> = WebDriverWait(driver, Duration.ofSeconds(2))
        wait.until { _ ->
            headingElement().getAttribute("aria-disabled") == null
        }
    }

    private fun headingElement() = driver.findElement(By.id(KB_SELECTOR_ID))
}