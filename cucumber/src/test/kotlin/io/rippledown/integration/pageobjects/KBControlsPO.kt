package io.rippledown.integration.pageobjects

import io.rippledown.constants.kb.*
import io.rippledown.integration.proxy.ConfiguredTestData
import java.io.File
import java.time.Duration

class KBControlsPO() {

    fun headingText(): String {
        TODO()
//        return headingElement().text!!
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
//        headingElement().click()
//        val exportButton = driver.findElement(By.id(KB_EXPORT_BUTTON_ID))
//        exportButton.click()
//        KBExportPO(driver).doExport()
    }

    private fun activateImportKB(): KBImportPO {
        TODO()
//        headingElement().click()
//        val importButton = driver.findElement(By.id(KB_IMPORT_BUTTON_ID))
//        importButton.click()
//        return KBImportPO(driver)
    }

    fun waitForKBToBeLoaded(name: String) {
//        val wait: Wait<WebDriver> = WebDriverWait(driver, Duration.ofSeconds(10))
//        wait.until { _ ->
//            kbName() == name
//        }
    }

    fun createKB(name: String) {
//        headingElement().click()
//        driver.findElement(By.id(KB_CREATE_MENU_ITEM_ID)).click()
//        driver.findElement(By.id(KB_CREATE_PROJECT_NAME_FIELD)).sendKeys(name)
//        driver.findElement(By.id(CONFIRM_CREATE_BUTTON_ID)).click()
    }

    fun requireKbControlsToBeDisabled() {
//        val wait: Wait<WebDriver> = WebDriverWait(driver, Duration.ofSeconds(2))
//        wait.until { _ ->
//            headingElement().getAttribute("aria-disabled") == "true"
//        }
    }

    fun requireKbControlsToBeEnabled() {
//        val wait: Wait<WebDriver> = WebDriverWait(driver, Duration.ofSeconds(2))
//        wait.until { _ ->
//            headingElement().getAttribute("aria-disabled") == null
//        }
    }

}