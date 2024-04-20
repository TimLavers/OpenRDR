package io.rippledown.integration.pageobjects

import io.kotest.assertions.fail
import io.rippledown.constants.caseview.CASELIST_ID
import io.rippledown.constants.caseview.CASE_NAME_PREFIX
import io.rippledown.constants.caseview.NUMBER_OF_CASES_ID
import io.rippledown.integration.pause
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findLabelChildren
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds
import java.util.concurrent.TimeUnit
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole.LABEL
import javax.accessibility.AccessibleRole.SCROLL_PANE
import javax.swing.SwingUtilities

class CaseListPO(private val contextProvider: () -> AccessibleContext) {

    fun waitForCountOfNumberOfCasesToBe(count: Int) {
        repeat(5) {
            val found = countTheNumberOfCasesEventThread()
            if (count == found) return
            pause(1000)
        }
        fail("Did not find $count cases, actual count: ${countOfTheNumberOfCases()}")
    }

    fun countTheNumberOfCasesEventThread(): Int? {
        val caseCount = mutableListOf<Int?>()
        SwingUtilities.invokeAndWait{ caseCount.add(countOfTheNumberOfCases())}
        return caseCount[0]
    }

    fun countOfTheNumberOfCases(): Int? {
        val textContext = contextProvider().find(NUMBER_OF_CASES_ID, LABEL)
        val name = textContext?.accessibleName
        val actualCount = name?.toInt()
        return actualCount
    }

    fun casesListed(): List<String> {
        val result = mutableListOf<String>()
        SwingUtilities.invokeAndWait {
            val caseNames = caseListContext()?.findLabelChildren()?: emptyList()
            result.addAll(caseNames)
        }
        return result
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
        await().pollDelay(ofSeconds(1)).atMost(ofSeconds(5)).until {
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