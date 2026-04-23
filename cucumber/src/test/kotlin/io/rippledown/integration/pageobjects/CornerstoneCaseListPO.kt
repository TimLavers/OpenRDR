package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.caseview.CASE_NAME_PREFIX
import io.rippledown.constants.caseview.CORNERSTONE_SECTION_ID
import io.rippledown.integration.utils.Cyborg
import io.rippledown.integration.utils.findAllByDescriptionPrefix
import io.rippledown.integration.utils.findExact
import io.rippledown.integration.utils.mouseClickAtCentre
import io.rippledown.integration.waitUntilAsserted
import org.assertj.swing.edt.GuiActionRunner.execute
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds
import javax.accessibility.AccessibleContext

class CornerstoneCaseListPO(private val contextProvider: () -> AccessibleContext) {

    private fun cornerstoneSectionContext() =
        execute<AccessibleContext?> { contextProvider().findExact(CORNERSTONE_SECTION_ID) }

    private fun waitTillCornerstoneSectionIsAccessible() =
        waitUntilAsserted { cornerstoneSectionContext() shouldNotBe null }

    private fun cornerstoneCasesListed(): List<String> {
        waitTillCornerstoneSectionIsAccessible()
        val section = cornerstoneSectionContext() ?: return emptyList()
        return execute<List<String>> {
            section.findAllByDescriptionPrefix(CASE_NAME_PREFIX)
                .map { it.accessibleDescription.removePrefix(CASE_NAME_PREFIX) }
        }
    }

    fun select(caseName: String) {
        waitForCornerstoneCaseListToContain(caseName)
        val caseNameContext = caseNameContext(caseName)!!
        execute {
            caseNameContext.accessibleAction.doAccessibleAction(0)
        }
    }

    /**
     * Performs an OS-level mouse click on the case. Use this when subsequent
     * Robot key presses must be routed to this case (e.g. for arrow-key
     * navigation); [select] goes through the accessibility API and does not
     * reliably move native focus when another compose focus request (such as
     * chat's LaunchedEffect) may race with it.
     */
    fun mouseClick(caseName: String) {
        waitForCornerstoneCaseListToContain(caseName)
        val ctx = caseNameContext(caseName) ?: return
        ctx.mouseClickAtCentre()
    }

    private fun caseNameContext(caseName: String): AccessibleContext? {
        val section = cornerstoneSectionContext() ?: return null
        return execute<AccessibleContext?> {
            section.findAllByDescriptionPrefix(CASE_NAME_PREFIX)
                .firstOrNull { it.accessibleDescription == "$CASE_NAME_PREFIX$caseName" }
        }
    }

    private fun waitForCornerstoneCaseListToContain(name: String) {
        await().atMost(ofSeconds(5)).until {
            cornerstoneCasesListed().contains(name)
        }
    }

    fun pressDownArrow() {
        Cyborg().downArrow()
    }

    fun pressUpArrow() {
        Cyborg().upArrow()
    }
}
