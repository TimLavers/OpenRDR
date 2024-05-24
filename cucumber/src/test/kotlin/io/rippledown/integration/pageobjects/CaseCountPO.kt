package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.caseview.NUMBER_OF_CASES_ID
import io.rippledown.integration.utils.find
import io.rippledown.integration.waitUntilAssertedOnEventThread
import org.assertj.swing.edt.GuiActionRunner.execute
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole.LABEL

class CaseCountPO(private val contextProvider: () -> AccessibleContext) {

    fun waitForCountOfNumberOfCasesToBe(count: Int) {
        if (countOfTheNumberOfCases() == count) return
        await().atMost(ofSeconds(60)).until {
            println("waiting: countOfTheNumberOfCases() = ${countOfTheNumberOfCases()}")
            countOfTheNumberOfCases() == count
        }
    }

    private fun countOfTheNumberOfCases(): Int {
        waitUntilAssertedOnEventThread { contextForCaseCount() shouldNotBe null }
        return execute<Int> { contextForCaseCount()?.accessibleName?.toInt() ?: 0 }
    }

    private fun contextForCaseCount() = contextProvider().find(NUMBER_OF_CASES_ID, LABEL)

    fun requireCaseCountToBeHidden() {
        contextForCaseCount() shouldBe null
    }

}