package io.rippledown.integration.pageobjects

import io.rippledown.integration.pause
import io.rippledown.integration.utils.Cyborg
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import java.awt.Robot
import java.io.File

class KBImportPO(private val driver: WebDriver) {

    fun selectZipAndDoImport(file: File) {
        val selectFileButton = driver.findElement(By.id("select_zip"))
        selectFileButton.click()
        pause(1200L) //Wait for the file selector to show.
        val cyborg = Cyborg()
        cyborg.enterText(file.absolutePath)
        pause(200)
        cyborg.enter()
    }
}