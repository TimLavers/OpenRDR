package io.rippledown.integration.pageobjects

import io.rippledown.integration.proxy.ConfiguredTestData
import org.awaitility.Awaitility
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import java.util.concurrent.TimeUnit

class KBInfoPO(private val driver: WebDriver) {

    fun headingText(): String? {
        return headingElement().text!!
    }

    fun kbName(): String? {
        val heading = headingText() ?: return null
        return heading.trim()
    }

    fun importKB(exportedFileName: String) {
        println("importKB: $exportedFileName")
        val zipFile = ConfiguredTestData.kbZipFile(exportedFileName)
        println("zipFile = ${zipFile.absolutePath}")
        activateImportKB().selectZipAndDoImport(zipFile)
    }

    private fun activateImportKB(): KBImportPO {
        val importButton = driver.findElement(By.id("import_from_zip"))
        importButton.click()
        return KBImportPO(driver)
    }

    fun waitForKBToBeLoaded(name: String) {
        Awaitility.await().atMost(5L, TimeUnit.SECONDS).until {
            kbName() == name
        }
    }

    private fun headingElement() = driver.findElement(By.id("kb_info_heading"))
}