package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.rippledown.constants.kb.KB_EXPORT_BUTTON_ID
import io.rippledown.constants.kb.KB_IMPORT_BUTTON_ID
import io.rippledown.constants.kb.KB_INFO_CONTROLS_ID
import io.rippledown.constants.kb.KB_INFO_HEADING_ID
import io.rippledown.integration.proxy.ConfiguredTestData
import org.awaitility.Awaitility
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import java.io.File
import java.util.concurrent.TimeUnit


class KBInfoPO(private val driver: WebDriver) {

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
        val exportButton = driver.findElement(By.id(KB_EXPORT_BUTTON_ID))
        exportButton.click()
        KBExportPO(driver).doExport()
    }

    private fun activateImportKB(): KBImportPO {
        val importButton = driver.findElement(By.id(KB_IMPORT_BUTTON_ID))
        importButton.click()
        return KBImportPO(driver)
    }

    fun waitForKBToBeLoaded(name: String) {
        Awaitility.await().atMost(10L, TimeUnit.SECONDS).until {
            kbName() == name
        }
    }

    fun requireKbControlsToBeHidden() {
        driver.findElements(By.id(KB_INFO_CONTROLS_ID)) shouldBe emptyList()
    }

    fun requireKbControlsToBeShown() {
        driver.findElement(By.id(KB_INFO_CONTROLS_ID)).isDisplayed shouldBe true
    }

    private fun headingElement() = driver.findElement(By.id(KB_INFO_HEADING_ID))
}