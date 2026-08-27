package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.caseview.CASELIST_ID
import io.rippledown.integration.utils.Cyborg
import io.rippledown.integration.utils.find
import io.rippledown.integration.waitUntilAsserted
import org.assertj.swing.edt.GuiActionRunner.execute
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole
import javax.accessibility.AccessibleRole.SCROLL_PANE

/**
 * The case list panel as a whole. Its sections all label their cases with the
 * one [io.rippledown.constants.caseview.CASE_NAME_PREFIX], and a case can
 * legitimately appear in more than one section (a cornerstone is normally also
 * a processed case), so a case is never addressed from here: use the
 * section-scoped [ProcessedCaseListPO], [CornerstoneCaseListPO] or
 * [FavouriteCaseListPO] for that. This page object covers only the concerns of
 * the whole panel.
 */
class CaseListPO(private val contextProvider: () -> AccessibleContext) {

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

    fun requireCaseListToBeHidden() {
        waitUntilAsserted { caseListContext() shouldBe null }
    }

    fun requireCaseListToBeShown() {
        waitUntilAsserted { caseListContext() shouldNotBe null }
    }

    fun pressDownArrow() {
        Cyborg().downArrow()
    }

    fun pressUpArrow() {
        Cyborg().upArrow()
    }
}