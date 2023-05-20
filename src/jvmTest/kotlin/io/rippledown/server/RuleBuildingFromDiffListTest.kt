package io.rippledown.server

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.rippledown.CaseTestUtils
import io.rippledown.model.COMMENT_SEPARATOR
import io.rippledown.model.CaseId
import io.rippledown.model.Interpretation
import io.rippledown.model.diff.*
import org.apache.commons.io.FileUtils
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class RuleBuildingFromDiffListTest {
    private lateinit var app: ServerApplication

    @BeforeTest
    fun setup() {
        app = ServerApplication()
        FileUtils.cleanDirectory(app.casesDir)
    }

    @Test
    fun `should build a rule and return an interpretation containing an updated DiffList when a comment is added`() {
        val id = "Case1"
        val caseId = CaseId(id, id)
        setUpCaseFromFile(id, app)
        app.case(id).interpretation.latestText() shouldBe "" //sanity check

        val verifiedText = "Verified 1. Verified 2."
        val verifiedInterpretation = Interpretation(
            caseId = caseId,
            verifiedText = verifiedText,
            diffList = DiffList(
                diffs = listOf(
                    Addition("Verified 1."),
                    Addition("Verified 2.")
                ),
                selected = 0 // The first addition is what we want the rule to be built from.
            )
        )
        app.buildRule(verifiedInterpretation)
        val updatedInterp = app.case(id).interpretation

        withClue("The returned DiffList should be updated to reflect the new rule.") {
            updatedInterp.diffList shouldBe DiffList(
                diffs = listOf(
                    Unchanged("Verified 1."),
                    Addition("Verified 2.")
                ),
                selected = -1
            )
        }
    }

    @Test
    fun `should build a rule and return an interpretation containing an updated DiffList when a comment is removed`() {
        val id = "Case1"
        val caseId = CaseId(id, id)
        setUpCaseFromFile(id, app)
        val comment1 = "Bondi or bust."
        val comment2 = "Bring your flippers."
        with(app) {
            startRuleSessionToAddConclusion(id, app.getOrCreateConclusion(comment1))
            commitCurrentRuleSession()
            startRuleSessionToAddConclusion(id, app.getOrCreateConclusion(comment2))
            commitCurrentRuleSession()
            case(id).interpretation.latestText() shouldBe "$comment1${COMMENT_SEPARATOR}$comment2" //sanity check
        }

        val verifiedText = comment1
        val verifiedInterpretation = Interpretation(
            caseId = caseId,
            verifiedText = verifiedText,
            diffList = DiffList(
                diffs = listOf(
                    Unchanged(comment1),
                    Removal(comment2)
                ),
                selected = 1 // We want the rule to be built for the removal
            )
        )
        app.buildRule(verifiedInterpretation)
        val updatedInterpretation = app.case(id).interpretation

        withClue("The returned DiffList should be updated to reflect the new rule.") {
            updatedInterpretation.diffList shouldBe DiffList(
                diffs = listOf(
                    Unchanged(comment1)
                ),
                selected = -1
            )
        }
    }

    @Test
    fun `should build a rule and return an interpretation containing an updated DiffList when a comment is replaced`() {
        val id = "Case1"
        val caseId = CaseId(id, id)
        setUpCaseFromFile(id, app)
        val comment1 = "Bondi or bust."
        val comment2 = "Bring your flippers."
        val comment3 = "Bring your snorkel."
        with(app) {
            startRuleSessionToAddConclusion(id, app.getOrCreateConclusion(comment1))
            commitCurrentRuleSession()
            startRuleSessionToAddConclusion(id, app.getOrCreateConclusion(comment2))
            commitCurrentRuleSession()
            case(id).interpretation.latestText() shouldBe "$comment1${COMMENT_SEPARATOR}$comment2" //sanity check
        }

        val verifiedText = "$comment1${COMMENT_SEPARATOR}$comment3"
        val verifiedInterpretation = Interpretation(
            caseId = caseId,
            verifiedText = verifiedText,
            diffList = DiffList(
                diffs = listOf(
                    Unchanged(comment1),
                    Replacement(comment2, comment3)
                ),
                selected = 1 // We want the rule to be built for the removal
            )
        )
        app.buildRule(verifiedInterpretation)
        val updatedInterpretation = app.case(id).interpretation

        withClue("The returned DiffList should be updated to reflect the new rule.") {
            updatedInterpretation.diffList shouldBe DiffList(
                diffs = listOf(
                    Unchanged(comment1),
                    Unchanged(comment3)
                ),
                selected = -1
            )
        }
    }

}

private fun setUpCaseFromFile(id: String, app: ServerApplication) {
    FileUtils.copyFileToDirectory(CaseTestUtils.caseFile(id), app.casesDir)
}
