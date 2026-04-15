package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.caseview.CASELIST_ID
import io.rippledown.constants.caseview.CASE_NAME_PREFIX
import io.rippledown.constants.caseview.CORNERSTONE_SECTION_ID
import io.rippledown.constants.caseview.PROCESSED_SECTION_ID
import io.rippledown.integration.utils.Cyborg
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findAllByDescriptionPrefix
import io.rippledown.integration.utils.findExact
import io.rippledown.integration.waitUntilAsserted
import org.assertj.swing.edt.GuiActionRunner.execute
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole
import javax.accessibility.AccessibleRole.LABEL
import javax.accessibility.AccessibleRole.SCROLL_PANE

class CaseListPO(private val contextProvider: () -> AccessibleContext) {
    fun casesListed(): List<String> {
        waitTillCaseListContextIsAccessible()
        val context = caseListContext() ?: return emptyList()
        return execute<List<String>> {
            context.findAllByDescriptionPrefix(CASE_NAME_PREFIX)
                .map { it.accessibleDescription.removePrefix(CASE_NAME_PREFIX) }
        }
    }

    private fun waitTillCaseListContextIsAccessible() =
        waitUntilAsserted { caseListContext() shouldNotBe null }

    private fun caseListContext(): AccessibleContext? {
        return execute<AccessibleContext?> {
            val provider = contextProvider()

            // Try finding without role first
            val foundNoRole = provider.find(CASELIST_ID)

            if (foundNoRole != null) {
                return@execute foundNoRole
            }

            // Try SCROLL_PANE
            val foundScroll = provider.find(CASELIST_ID, SCROLL_PANE)

            if (foundScroll != null) {
                return@execute foundScroll
            }

            // Try PANEL
            val panelFound = provider.find(CASELIST_ID, AccessibleRole.PANEL)

            panelFound
        }
    }

    fun requireCaseNamesToBe(expectedCaseNames: List<String>) {
        casesListed() shouldBe expectedCaseNames
    }

    fun select(caseName: String) {
        waitForCaseListToContain(caseName)
        requireCaseToBeShown(caseName)
        val caseNameContext = caseNameContext(caseName)!!
        execute {
            caseNameContext.accessibleAction.doAccessibleAction(0)
        }
    }

    private fun caseNameContext(caseName: String) = contextProvider().find("$CASE_NAME_PREFIX$caseName", LABEL)

    fun waitForCaseListToContain(name: String) {
        await().atMost(ofSeconds(5)).until {
            casesListed().contains(name)
        }
    }

    fun requireCaseListToBeHidden() {
        waitUntilAsserted { caseListContext() shouldBe null }
    }

    fun requireCaseListToBeShown() {
        waitUntilAsserted { caseListContext() shouldNotBe null }
    }
    fun requireCaseToBeShown(caseName: String) {
        waitUntilAsserted { caseNameContext(caseName) shouldNotBe null }
    }

    fun requireCornerstoneCaseNamesToBe(expectedCaseNames: List<String>) {
        waitUntilAsserted {
            val section = execute<AccessibleContext?> { contextProvider().findExact(CORNERSTONE_SECTION_ID) }
            section shouldNotBe null
            val names = execute<List<String>> { caseNamesInSection(section!!) }
            names shouldBe expectedCaseNames
        }
    }

    fun requireProcessedCaseNamesToBe(expectedCaseNames: List<String>) {
        waitUntilAsserted {
            val section = execute<AccessibleContext?> { contextProvider().findExact(PROCESSED_SECTION_ID) }
            section shouldNotBe null
            val names = execute<List<String>> { caseNamesInSection(section!!) }
            names shouldBe expectedCaseNames
        }
    }

    private fun caseNamesInSection(section: AccessibleContext): List<String> {
        return section.findAllByDescriptionPrefix(CASE_NAME_PREFIX)
            .map { it.accessibleDescription.removePrefix(CASE_NAME_PREFIX) }
    }

    fun pressDownArrow() {
        Cyborg().downArrow()
    }

    fun pressUpArrow() {
        Cyborg().upArrow()
    }
}