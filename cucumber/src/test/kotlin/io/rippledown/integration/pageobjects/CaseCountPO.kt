package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.caseview.PROCESSED_SECTION_HEADER_ID
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

    private val countPattern = Regex("\\((\\d+)\\)")

    private fun countOfTheNumberOfCases(): Int {
        val context = contextForCaseCount() ?: return 0
        return execute<Int> {
            val name = context.accessibleName ?: ""
            countPattern.find(name)?.groupValues?.get(1)?.toInt() ?: 0
        }
    }

    private fun contextForCaseCount() =
        execute<AccessibleContext?> { contextProvider().find(PROCESSED_SECTION_HEADER_ID) }

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