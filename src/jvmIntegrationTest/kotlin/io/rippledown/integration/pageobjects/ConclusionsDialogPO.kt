package io.rippledown.integration.pageobjects

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

class ConclusionsDialogPO(private val driver: WebDriver) {

    private fun dialogElement() = driver.findElement(By.id("conclusions_dialog"))

    fun clickOpen() {
        driver.findElement(By.id("conclusions_dialog_open")).click()
    }

    fun clickClose() {
        driver.findElement(By.id("conclusions_dialog_close")).click()
    }

    fun waitForDialogToOpen() =
        WebDriverWait(driver, Duration.ofSeconds(2)).until {
            dialogElement().isDisplayed
        }

    fun waitForDialogToClose() =
        WebDriverWait(driver, Duration.ofSeconds(2)).until {
            dialogIsClosed()
        }

    fun clickComment(comment: String) {
        driver.findElement(By.ByXPath("//*[contains(@id, '$comment')]")).click()
    }

    fun requireConditionsToBeShown(vararg conditions: String) {
        conditions.forEach { condition ->
            driver.findElement(By.ByXPath("//*[contains(@id, '$condition')]"))
        }
    }

    private fun dialogIsClosed(): Boolean {
        return try {
            dialogElement()
            false
        } catch (e: Exception) {
            //Expected
            true
        }
    }

}