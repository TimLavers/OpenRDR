package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.caseview.CASE_NAME_PREFIX
import io.rippledown.integration.utils.Cyborg
import io.rippledown.integration.utils.findAllByDescriptionPrefix
import io.rippledown.integration.utils.findExact
import io.rippledown.integration.utils.mouseClickAtCentre
import io.rippledown.integration.waitUntilAsserted
import org.assertj.swing.edt.GuiActionRunner.execute
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds
import javax.accessibility.AccessibleContext

/**
 * Base page object for a single named section of the case list (e.g.
 * Processed, Cornerstones, Favourites) as rendered by
 * [io.rippledown.casecontrol.CaseSelector]. Subclasses need only supply the
 * section's accessibility description via [sectionDescription].
 */
abstract class AbstractCaseSectionListPO(private val contextProvider: () -> AccessibleContext) {

    abstract fun sectionDescription(): String

    private fun sectionContext(): AccessibleContext? =
        execute<AccessibleContext?> { contextProvider().findExact(sectionDescription()) }

    private fun waitTillSectionIsAccessible() =
        waitUntilAsserted { sectionContext() shouldNotBe null }

    fun casesListed(): List<String> {
        waitTillSectionIsAccessible()
        val section = sectionContext() ?: return emptyList()
        return execute<List<String>> {
            section.findAllByDescriptionPrefix(CASE_NAME_PREFIX)
                .map { it.accessibleDescription.removePrefix(CASE_NAME_PREFIX) }
        }
    }

    fun requireCaseNamesToBe(expectedCaseNames: List<String>) {
        waitUntilAsserted { casesListed() shouldBe expectedCaseNames }
    }

    fun select(caseName: String) {
        waitForCaseListToContain(caseName)
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
        waitForCaseListToContain(caseName)
        val ctx = caseNameContext(caseName) ?: return
        ctx.mouseClickAtCentre()
    }

    private fun caseNameContext(caseName: String): AccessibleContext? {
        val section = sectionContext() ?: return null
        return execute<AccessibleContext?> {
            section.findAllByDescriptionPrefix(CASE_NAME_PREFIX)
                .firstOrNull { it.accessibleDescription == "$CASE_NAME_PREFIX$caseName" }
        }
    }

    fun waitForCaseListToContain(name: String) {
        await().atMost(ofSeconds(5)).until {
            casesListed().contains(name)
        }
    }

    fun pressDownArrow() {
        Cyborg().downArrow()
    }

    fun pressUpArrow() {
        Cyborg().upArrow()
    }
}
