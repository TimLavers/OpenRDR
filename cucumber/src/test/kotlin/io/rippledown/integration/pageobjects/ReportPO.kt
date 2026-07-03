package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.interpretation.REPORT_PANEL
import io.rippledown.constants.interpretation.REPORT_TEXT
import io.rippledown.constants.interpretation.REPORT_TOGGLE
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.mouseClickAtCentre
import io.rippledown.integration.waitUntilAsserted
import org.assertj.swing.edt.GuiActionRunner.execute
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds
import javax.accessibility.AccessibleContext

class ReportPO(private val contextProvider: () -> AccessibleContext) {

    fun clickReportToggle() {
        waitUntilAsserted {
            execute<AccessibleContext?> { contextProvider().find(REPORT_TOGGLE) } shouldNotBe null
        }
        // The toggle is a clickable Compose Row which does not expose an
        // AccessibleAction, so doAccessibleAction() would NPE. Perform an
        // OS-level mouse click at the row's centre instead.
        contextProvider().find(REPORT_TOGGLE)!!.mouseClickAtCentre()
    }

    fun waitForReportPanelToBeVisible() {
        await().atMost(ofSeconds(10)).until {
            execute<AccessibleContext?> { contextProvider().find(REPORT_PANEL) } != null
        }
    }

    fun waitForReportPanelToBeHidden() {
        await().atMost(ofSeconds(10)).until {
            execute<AccessibleContext?> { contextProvider().find(REPORT_PANEL) } == null
        }
    }

    fun reportText(): String =
        execute<String> {
            // From Compose 1.11 the Java accessibility bridge uses the
            // contentDescription as the accessible name on Text nodes,
            // overriding the rendered text. Read the rendered text via
            // AccessibleText (which exposes the actual characters) instead.
            val ctx = contextProvider().find(REPORT_TEXT) ?: return@execute ""
            val text = ctx.accessibleText ?: return@execute ctx.accessibleName ?: ""
            buildString {
                for (i in 0 until text.charCount) {
                    val ch = text.getAtIndex(javax.accessibility.AccessibleText.CHARACTER, i)
                    if (ch != null) append(ch)
                }
            }
        }

    fun waitForReportTextToContain(phrases: List<String>) {
        await().atMost(ofSeconds(30)).until {
            val text = reportText()
            phrases.all { text.contains(it) }
        }
    }
}
