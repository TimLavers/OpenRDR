package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.rippledown.constants.caseview.CASELIST_ID
import io.rippledown.constants.caseview.CASES
import io.rippledown.constants.caseview.CASE_NAME_PREFIX
import io.rippledown.constants.caseview.NUMBER_OF_CASES_ID
import org.awaitility.Awaitility.await
import org.awaitility.kotlin.withPollInterval
import java.time.Duration.ofSeconds
import java.util.concurrent.TimeUnit

class CaseListPO() {

    private fun containerElement() {
        TODO()
    }

    fun waitForCaseListToHaveSize(count: Int) {
//        await().atMost(5L, TimeUnit.SECONDS).until {
//            casesListed().size == count
//        }
    }

    fun waitForCountOfNumberOfCasesToBe(count: Int) {
//        val wait: Wait<WebDriver> = WebDriverWait(driver, ofSeconds(5))
//        wait.until { _ ->
//            countOfTheNumberOfCases() == count
//        }
    }

    fun countOfTheNumberOfCases() {
        TODO()
    }
//        driver.findElement(By.id(NUMBER_OF_CASES_ID)).text
//        .substringAfter("$CASES ").toInt()

    fun waitForCaseListToContain(name: String) {
//        val wait: Wait<WebDriver> = WebDriverWait(driver, ofSeconds(5))
//        wait.until { _ ->
//            driver.findElement(By.id("$CASE_NAME_PREFIX$name")).isDisplayed
//        }
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

    fun casesListed() {
        TODO()
    }
//    = listItems().map { it.text }

    private fun listItems()  {
        TODO()
    }
//        containerElement().findElements(By.className("MuiListItemButton-root"))

    fun select(caseName: String): CaseViewPO {
        TODO()
//        val id = "$CASE_NAME_PREFIX$caseName"
//        driver.findElement(By.id(id)).click()
//        return CaseViewPO(driver)
    }

    fun requireCaseCountToBeHidden() {
//        driver.findElements(By.id(NUMBER_OF_CASES_ID)) shouldBe emptyList()
    }

    fun requireCaseCountToBe(expected: Int) {
//        driver.findElements(By.id(NUMBER_OF_CASES_ID)) shouldBe emptyList()
    }
}