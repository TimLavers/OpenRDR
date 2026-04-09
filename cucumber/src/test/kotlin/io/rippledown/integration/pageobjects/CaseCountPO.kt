package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.caseview.NUMBER_OF_CASES_ID
import io.rippledown.integration.utils.find
import io.rippledown.integration.waitUntilAsserted
import org.assertj.swing.edt.GuiActionRunner.execute
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds
import javax.accessibility.AccessibleContext

class CaseCountPO(private val contextProvider: () -> AccessibleContext) {

    fun waitForCountOfNumberOfCasesToBe(count: Int) {
        if (countOfTheNumberOfCases() == count) return
        await().atMost(ofSeconds(120)).until {
            countOfTheNumberOfCases() == count
        }
    }

    private fun countOfTheNumberOfCases(): Int {
        waitUntilAsserted { contextForCaseCount() shouldNotBe null }
        return execute<Int> {
            contextForCaseCount()?.accessibleName
                ?.substringBefore(" ")
                ?.toIntOrNull() ?: 0
        }
    }

    private fun contextForCaseCount() = execute<AccessibleContext?> { contextProvider().find(NUMBER_OF_CASES_ID) }

    fun requireCaseCountToBeHidden() {
        waitUntilAsserted { contextForCaseCount() shouldBe null }
    }

    fun requireCasesLabelToBeHidden() {
        waitUntilAsserted { contextForCaseCount() shouldBe null }
    }

    fun requireCasesLabelToBeShown() {
        waitUntilAsserted { contextForCaseCount() shouldNotBe null }
    }

    fun requireCaseCountToBeShown() {
        waitUntilAsserted { contextForCaseCount() shouldNotBe null }
    }

}