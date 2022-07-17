package io.rippledown.integration.pageobjects

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver

class CaseQueuePO(private val driver: WebDriver) {

    fun numberWaiting(): Int {
        val waitingCasesElement = driver.findElement(By.id("number_of_cases_waiting_value"))
        return Integer.parseInt(waitingCasesElement.text)
    }

    fun refresh() {
        val refreshButton = driver.findElement(By.id("refresh_waiting_cases_info_button"))
        refreshButton.click()
    }
}