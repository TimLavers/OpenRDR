package io.rippledown.integration.pageobjects

import io.rippledown.constants.caseview.CASELIST_ID
import io.rippledown.constants.caseview.CASES
import io.rippledown.constants.caseview.CASE_NAME_PREFIX
import io.rippledown.constants.caseview.NUMBER_OF_CASES_ID
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findLabelChildren
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds
import java.util.concurrent.TimeUnit
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole.LABEL
import javax.accessibility.AccessibleRole.SCROLL_PANE

class CaseListPO(private val contextProvider: () -> AccessibleContext) {

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

    fun casesListed(): List<String> {
        return caseListContext()?.findLabelChildren()?: emptyList()
    }

    fun requireCaseNamesToBe(expectedCaseNames: List<String>) {
        await().atMost(5L, TimeUnit.SECONDS).until {
            casesListed() == expectedCaseNames
        }
    }

    fun select(caseName: String): CaseViewPO {
        waitForCaseListToContain(caseName)
        caseNameContext(caseName)!!.accessibleAction.doAccessibleAction(0)
        return CaseViewPO {
            contextProvider()
        }
    }

    private fun caseNameContext(caseName: String) = contextProvider().find("$CASE_NAME_PREFIX$caseName", LABEL)

    private fun caseListContext() = contextProvider().find(CASELIST_ID, SCROLL_PANE)

    fun waitForCaseListToContain(name: String) {
        await().atMost(ofSeconds(5)).until {
            casesListed().contains(name)
        }
    }

    fun waitForNoCases() {
        await().atMost(ofSeconds(5)).until {
            casesListed().isEmpty()
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