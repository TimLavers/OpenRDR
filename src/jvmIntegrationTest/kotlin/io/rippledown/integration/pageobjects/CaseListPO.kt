package io.rippledown.integration.pageobjects

import org.awaitility.Awaitility.await
import org.awaitility.kotlin.withPollInterval
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import java.time.Duration.ofSeconds
import java.util.concurrent.TimeUnit

class CaseListPO(private val driver: WebDriver) {

    private fun containerElement() = driver.findElement(By.id("case_list_container"))

    fun waitForCaseListToHaveSize(count: Int) {
        await().atMost(5L, TimeUnit.SECONDS).until {
            casesListed().size == count
        }
    }

    fun waitForNoCases() {
        await().atMost(ofSeconds(15))
            .withPollInterval(ofSeconds(2)).until {
                val containerElement = try {
                    containerElement()
                } catch (e: Exception) {
                    null
                }
                containerElement == null
            }
    }

    fun casesListed() = listItems().map { it.text }

    private fun listItems() = containerElement().findElements(By.className("MuiTypography-root"))

    fun select(caseName: String): CaseViewPO {
        val id = "case_list_item_$caseName"
        driver.findElement(By.id(id)).click()
        return CaseViewPO(driver)
    }
}