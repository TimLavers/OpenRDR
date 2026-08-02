package io.rippledown.kb.report

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.rippledown.model.*
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.model.rule.*
import io.rippledown.utils.defaultDate
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ReportServiceTest {

    private val reportService = ReportService()

    @Test
    fun `userContent should include comments section`() {
        val caseId = CaseId(1, "Test")
        val interpretation = Interpretation(caseId)
        val case = RDRCase(caseId, emptyMap(), interpretation)
        val viewProperties = CaseViewProperties(case.attributes.toList())
        val viewableCase = ViewableCase(case, viewProperties)

        val content = reportService.userContent(viewableCase) { null }
        content shouldContain "Comments"
    }

    @Test
    fun `userContent should include serialized case data`() {
        val caseId = CaseId(1, "Test")
        val interpretation = Interpretation(caseId)
        val attribute = Attribute(1, "HGB")
        val event = Event(attribute, 1000L)
        val data = mapOf(event to io.rippledown.model.Result("194"))
        val case = RDRCase(caseId, data, interpretation)
        val viewProperties = CaseViewProperties(listOf(attribute))
        val viewableCase = ViewableCase(case, viewProperties)

        val content = reportService.userContent(viewableCase) { null }
        content shouldContain "Case data (JSON)"
        content shouldContain "HGB"
    }

    @Test
    fun `userContent should serialize multi-episode multi-attribute case with comments`() {
        // Build a multi-episode case with multiple attributes (similar to Einstein)
        val hgb = Attribute(101, "HAEMOGLOBIN")
        val mcv = Attribute(102, "MCV")
        val sodium = Attribute(103, "Sodium")

        val builder = RDRCaseBuilder()

        // Episode 1 - first date
        val date1 = defaultDate
        builder.addValue(hgb, date1, "194")
        builder.addValue(mcv, date1, "100.2")
        builder.addValue(sodium, date1, "141")

        // Episode 2 - second date (6 months later)
        val date2 = date1 + (180L * 24 * 60 * 60 * 1000)
        builder.addValue(hgb, date2, "188")
        builder.addValue(mcv, date2, "99.5")
        builder.addValue(sodium, date2, "140")

        val case = builder.build("Einstein-like", 1)
        val interpretation = case.interpretation

        // Add rules to create comments
        val conclusion1 = io.rippledown.model.Conclusion(1, "High HGB")
        val conclusion2 = io.rippledown.model.Conclusion(2, "Macrocytosis")
        val rule1 = io.rippledown.model.rule.Rule(1, conclusion = conclusion1)
        val rule2 = io.rippledown.model.rule.Rule(2, conclusion = conclusion2)
        interpretation.add(rule1)
        interpretation.add(rule2)

        val viewProperties = CaseViewProperties(listOf(hgb, mcv, sodium))
        val viewableCase = ViewableCase(case, viewProperties)

        val content = reportService.userContent(viewableCase) { id ->
            when (id) {
                101 -> hgb
                102 -> mcv
                103 -> sodium
                else -> null
            }
        }

        // Verify comments are included
        content shouldContain "Comments"
        content shouldContain "High HGB"
        content shouldContain "Macrocytosis"

        // Verify JSON serialization includes case data
        content shouldContain "Case data (JSON)"
        content shouldContain "HAEMOGLOBIN"
        content shouldContain "MCV"
        content shouldContain "Sodium"
        content shouldContain "194"
        content shouldContain "188"
        content shouldContain "100.2"
        content shouldContain "99.5"
    }

    @Test
    fun `generate should return empty report when comments are blank`() = runTest {
        val caseId = CaseId(1, "Test")
        val emptyInterpretation = Interpretation(caseId)
        val case = RDRCase(caseId, emptyMap(), emptyInterpretation)
        val viewProperties = CaseViewProperties(case.attributes.toList())
        val emptyViewable = ViewableCase(case, viewProperties)

        val report = reportService.generate(emptyViewable) { null }
        report.markdown shouldBe ""
        report.generated shouldBe false
    }

    @Test
    fun `userContent should include comments given as comment-attribute assignments`() = runTest {
        // Given a case whose raw interpretation holds an unresolved ByDefinition
        // comment assignment, and whose viewable interpretation holds the
        // resolved copy, as produced by KB.viewableCase
        val viewableCase = viewableCaseWithCommentAssignment(
            rawExpression = ByDefinition,
            resolvedExpression = CommentTemplate("Diabetic diet advice given.")
        )

        // When the LLM user content is built
        val content = reportService.userContent(viewableCase) { null }

        // Then the resolved comment is included
        content shouldContain "Diabetic diet advice given."
    }

    @Test
    fun `generate should not produce a report when the only comment assignment is unresolved`() = runTest {
        // Given a case whose comment assignment could not be resolved to a definition
        val viewableCase = viewableCaseWithCommentAssignment(
            rawExpression = ByDefinition,
            resolvedExpression = ByDefinition
        )

        // When a report is generated
        val report = reportService.generate(viewableCase) { null }

        // Then there are no comments, so no report
        report.markdown shouldBe ""
        report.generated shouldBe false
    }

    private fun viewableCaseWithCommentAssignment(
        rawExpression: ValueExpression,
        resolvedExpression: ValueExpression
    ): ViewableCase {
        val caseId = CaseId(1, "Test")
        val c1 = Attribute(10, "C1", AttributeKind.COMMENT)
        val rawInterpretation = Interpretation(caseId).apply {
            add(RuleSummary(id = 1, assignment = AssignValue(c1, rawExpression)))
        }
        val resolvedInterpretation = Interpretation(caseId).apply {
            add(RuleSummary(id = 1, assignment = AssignValue(c1, resolvedExpression)))
        }
        val case = RDRCase(caseId, emptyMap(), rawInterpretation)
        val viewProperties = CaseViewProperties(case.attributes.toList())
        return ViewableCase(case, viewProperties, ViewableInterpretation(resolvedInterpretation))
    }
}
