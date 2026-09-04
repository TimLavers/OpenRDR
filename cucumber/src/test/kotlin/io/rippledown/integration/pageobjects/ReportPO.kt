package io.rippledown.integration.pageobjects

import io.rippledown.constants.interpretation.REPORT_PANEL
import io.rippledown.constants.interpretation.REPORT_TEXT
import io.rippledown.constants.interpretation.REPORT_TOGGLE
import io.rippledown.cornerstone.CornerstoneTestHook
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.performAccessibleClick
import org.assertj.swing.edt.GuiActionRunner.execute
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds
import javax.accessibility.AccessibleContext

class ReportPO(private val contextProvider: () -> AccessibleContext) {

    fun clickReportToggle() {
        // Wait for any rule session to complete (no cornerstone showing)
        await().atMost(ofSeconds(30)).until {
            execute<Boolean> { !CornerstoneTestHook.snapshot().isShowing }
        }
        await().atMost(ofSeconds(15)).until {
            execute<AccessibleContext?> { contextProvider().find(REPORT_TOGGLE) } != null
        }
        execute<Unit> {
            val toggle = contextProvider().find(REPORT_TOGGLE)
            if (toggle != null) {
                check(toggle.performAccessibleClick()) {
                    "Report toggle has no invokable accessibility action"
                }
            } else {
                throw IllegalStateException("Report toggle not found in accessibility tree")
            }
        }
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
            // The markdown renderer does not expose its rendered text to the
            // Java accessibility bridge, so ReportView publishes the raw report
            // markdown on a hidden, zero-size Text node carrying REPORT_TEXT.
            // Read the characters from its AccessibleText.
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
            phrases.all { text.contains(it, true) }
        }
    }
}
