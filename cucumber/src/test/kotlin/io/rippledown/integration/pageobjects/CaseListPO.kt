package io.rippledown.integration.pageobjects

import io.rippledown.constants.caseview.CASELIST_ID
import io.rippledown.constants.caseview.CASES
import io.rippledown.constants.caseview.CASE_NAME_PREFIX
import io.rippledown.constants.caseview.NUMBER_OF_CASES_ID
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findLabelChildren
import org.awaitility.Awaitility.await
import org.awaitility.kotlin.withPollInterval
import java.time.Duration.ofSeconds
import java.util.concurrent.TimeUnit
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole.LABEL
import javax.accessibility.AccessibleRole.SCROLL_PANE

class CaseListPO(private val contextProvider: () -> AccessibleContext) {

    private fun containerElement() {
        TODO()
    }

    fun waitForCaseListToHaveSize(count: Int) {
        await().atMost(5L, TimeUnit.SECONDS).until {
            val found = caseCount()
            found != null && found == count
        }
    }

    private fun caseCount(): Int? {
        TODO()
    }

    fun waitForCountOfNumberOfCasesToBe(count: Int) {
        await().atMost(5L, TimeUnit.SECONDS).until {
            val found = countOfTheNumberOfCases()
            found != null && found == count
        }
    }

    fun countOfTheNumberOfCases(): Int? {
        val textContext = contextProvider().find(NUMBER_OF_CASES_ID, LABEL)
        val name = textContext?.accessibleName
        val actualCount = name?.substringAfter("$CASES ")?.toInt()
        return actualCount
    }

    fun casesListed(): List<String>? {
        val scrollPaneContext = caseListContext()
        return scrollPaneContext?.findLabelChildren()
    }

    fun requireCaseNamesToBe(expectedCaseNames: List<String>) {
        await().atMost(5L, TimeUnit.SECONDS).until {
            val found = casesListed()
            found != null && found == expectedCaseNames
        }
    }

    fun select(caseName: String): CaseViewPO {
        println("finding case name $CASE_NAME_PREFIX$caseName")
        caseNameContext(caseName).accessibleAction.doAccessibleAction(0)
        return CaseViewPO(contextProvider)
    }

    private fun caseNameContext(caseName: String): AccessibleContext {
        var caseNameContext: AccessibleContext? = null
        await().atMost(5L, TimeUnit.SECONDS).until {
            caseNameContext = caseListContext().find(description = "$CASE_NAME_PREFIX$caseName", role = LABEL)
            caseNameContext != null
        }
        return caseNameContext!!
    }


    private fun caseListContext(): AccessibleContext {
        await().atMost(5L, TimeUnit.SECONDS).until {
            val found = contextProvider().find(CASELIST_ID, SCROLL_PANE)
            found != null
        }
        return contextProvider().find(description = CASELIST_ID, role = SCROLL_PANE)!!
    }
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

//    = listItems().map { it.text }
private fun listItems() {
        TODO()
    }

//        containerElement().findElements(By.className("MuiListItemButton-root"))

    fun requireCaseCountToBeHidden() {
//        driver.findElements(By.id(NUMBER_OF_CASES_ID)) shouldBe emptyList()
    }

    fun requireCaseCountToBe(expected: Int) {
//        driver.findElements(By.id(NUMBER_OF_CASES_ID)) shouldBe emptyList()
    }
}