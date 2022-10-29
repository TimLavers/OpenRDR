package io.rippledown.integration.pageobjects

import org.awaitility.Awaitility.await
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import java.util.concurrent.TimeUnit

class CaseListPO(private val driver: WebDriver) {

    private fun containerElement() = driver.findElement(By.id("case_list_container"))

    fun waitForCaseListToHaveSize(count: Int) {
        await().atMost(5L, TimeUnit.SECONDS).until {
            casesListed().size == count
        }
    }

    fun casesListed(): List<String> {
        return containerElement().findElements(By.tagName("li")).map { it.text }
    }

    fun select(caseName: String): CaseViewPO {
        val id = "case_list_item_$caseName"
        driver.findElement(By.id(id)).click()
        return CaseViewPO(driver)
    }
}