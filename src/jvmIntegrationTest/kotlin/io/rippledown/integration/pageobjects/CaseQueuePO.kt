package io.rippledown.integration.pageobjects

import org.awaitility.Awaitility.await
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import java.util.concurrent.TimeUnit

class CaseQueuePO(private val driver: WebDriver) {

    fun waitForNumberWaitingToBe(count: Int): CaseQueuePO {
        await().atMost(5, TimeUnit.SECONDS).until {
            numberWaiting() == count
        }
        return this
    }

    fun numberWaiting() = driver.findElement(By.id("number_of_cases_waiting_value"))
        .text
        .substringAfter("Cases waiting: ")
        .toInt()
}
