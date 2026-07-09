package io.rippledown.kb.report

import io.rippledown.llm.generateText
import io.rippledown.llm.retry
import io.rippledown.model.Attribute
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.report.CaseReport
import io.rippledown.toJsonString

class ReportService {
    private fun readPrompt(): String =
        (javaClass.getResource("/report/report_system_prompt.md")
            ?: error("report_system_prompt.md not found")).readText()

    /** Build the LLM user-content from the case's comments (+ serialized case data). */
    fun userContent(viewableCase: ViewableCase, attributeById: (Int) -> Attribute?): String {
        val comments = viewableCase.case.interpretation.toComments(viewableCase.case, attributeById)
        val caseJson = viewableCase.toJsonString()
        return "Comments (JSON array):\n$comments\n\nCase data (JSON):\n$caseJson"
    }

    suspend fun generate(viewableCase: ViewableCase, attributeById: (Int) -> Attribute?): CaseReport {
        val comments = viewableCase.case.interpretation.toComments(viewableCase.case, attributeById)
        // toComments returns "[]" for an empty interpretation — do not call the LLM.
        if (comments.isBlank() || comments == "[]") return CaseReport(markdown = "", generated = false)
        // This is an interactive, UI-triggered path, so cap retries and per-call
        // timeout to keep worst-case latency bounded rather than using the
        // longer defaults intended for batch work.
        val text = retry(maxRetries = 3) {
            generateText(readPrompt(), userContent(viewableCase, attributeById), timeoutMs = 30_000)
        }
        return CaseReport(markdown = text.trim(), generated = true)
    }
}
