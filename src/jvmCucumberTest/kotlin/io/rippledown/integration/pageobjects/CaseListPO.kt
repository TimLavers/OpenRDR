package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.rippledown.constants.caseview.CASELIST_ID
import io.rippledown.constants.caseview.CASE_NAME_PREFIX
import io.rippledown.constants.caseview.NUMBER_OF_CASES_ID
import org.awaitility.Awaitility.await
import org.awaitility.kotlin.withPollInterval
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.Wait
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration.ofSeconds
import java.util.concurrent.TimeUnit


class CaseListPO(private val driver: WebDriver) {

    private fun containerElement() = driver.findElement(By.id(CASELIST_ID))

    fun waitForCaseListToHaveSize(count: Int) {
        await().atMost(5L, TimeUnit.SECONDS).until {
            casesListed().size == count
        }
    }

    fun waitForCaseListToContain(name: String) {
        val wait: Wait<WebDriver> = WebDriverWait(driver, ofSeconds(5))
        wait.until { _ ->
            driver.findElement(By.id("$CASE_NAME_PREFIX$name")).isDisplayed
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

    private fun listItems() = containerElement().findElements(By.className("MuiListItemButton-root"))

    fun select(caseName: String): CaseViewPO {
        val id = "$CASE_NAME_PREFIX$caseName"
        driver.findElement(By.id(id)).click()
        return CaseViewPO(driver)
    }

    fun requireCaseCountToBeHidden() {
        driver.findElements(By.id(NUMBER_OF_CASES_ID)) shouldBe emptyList()
    }
}