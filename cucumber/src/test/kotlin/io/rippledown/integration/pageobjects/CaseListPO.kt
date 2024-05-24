package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.caseview.CASELIST_ID
import io.rippledown.constants.caseview.CASE_NAME_PREFIX
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findLabelChildren
import io.rippledown.integration.waitUntilAssertedOnEventThread
import org.assertj.swing.edt.GuiActionRunner.execute
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole.LABEL
import javax.accessibility.AccessibleRole.SCROLL_PANE

class CaseListPO(private val contextProvider: () -> AccessibleContext) {

    private lateinit var caseNamesCached: List<String>

    fun loadAllCaseNames() {
        caseNamesCached = casesListed()
    }

    private fun casesListed(): List<String> {
        if (::caseNamesCached.isInitialized) return caseNamesCached
        else {
            waitTillCaseListContextIsAccessible()
            println("start loading all case names ${System.currentTimeMillis()}")
            caseNamesCached = execute<List<String>> { caseListContext()?.findLabelChildren() ?: emptyList() }
            println("end   loading all case names ${System.currentTimeMillis()}")
        }
        return caseNamesCached
    }

    private fun waitTillCaseListContextIsAccessible() =
        waitUntilAssertedOnEventThread {
            caseListContext() shouldNotBe null
        }

    private fun caseListContext() = contextProvider().find(CASELIST_ID, SCROLL_PANE)


    fun requireCaseNamesToBe(expectedCaseNames: List<String>) {
        caseNamesCached shouldBe expectedCaseNames
    }

    fun select(caseName: String) {
        waitForCaseListToContain(caseName)
        execute { caseNameContext(caseName)!!.accessibleAction.doAccessibleAction(0) }
    }

    private fun caseNameContext(caseName: String) = contextProvider().find("$CASE_NAME_PREFIX$caseName", LABEL)

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
}