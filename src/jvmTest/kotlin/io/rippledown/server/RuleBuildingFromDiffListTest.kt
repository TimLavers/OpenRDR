package io.rippledown.server

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.rippledown.CaseTestUtils
import io.rippledown.model.COMMENT_SEPARATOR
import io.rippledown.model.diff.*
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
import io.rippledown.persistence.inmemory.InMemoryPersistenceProvider
import org.apache.commons.io.FileUtils
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class RuleBuildingFromDiffListTest {
    private lateinit var app: ServerApplication

    @BeforeTest
    fun setup() {
        app = ServerApplication(InMemoryPersistenceProvider())
        FileUtils.cleanDirectory(app.casesDir)
    }

    @Test
    fun `should return empty CornerstoneStatus when a rule session is started and there are no cornerstones`() {
        val id = "Case1"
        setUpCaseFromFile(id, app)
        val diff = Addition("Go to Bondi")
        val cornerstoneStatus = app.startRuleSession(SessionStartRequest(id, diff))
        cornerstoneStatus shouldBe CornerstoneStatus()
    }

    @Test
    fun `should return the first cornerstone when a rule session is started and there are cornerstones`() {
        val id1 = "Case1"
        val id2 = "Case2"
        setUpCaseFromFile(id1, app)
        setUpCaseFromFile(id2, app)
        val case1 = app.case(id1)
        val case2 = app.case(id2)
        app.kb.addCornerstoneCase(case1)
        app.kb.addCornerstoneCase(case2)
        val viewableCase = app.viewableCase(id1)
        val diff = Addition("Go to Bondi")
        val cornerstoneStatus = app.startRuleSession(SessionStartRequest(id2, diff))
        cornerstoneStatus shouldBe CornerstoneStatus(viewableCase, 0, 1)
    }

    @Test
    fun `should build a rule and return an interpretation containing an updated DiffList when a comment is added`() {
        val id = "Case1"
        setUpCaseFromFile(id, app)

        val interp = app.case(id).interpretation
        val v1 = "Verified 1."
        val v2 = "Verified 2."
        interp.verifiedText = "$v1 $v2"
        interp.latestText() shouldBe "$v1 $v2"

        val diffList = DiffList(
            diffs = listOf(
                Addition(v1),
                Addition(v2)
            ),
            selected = 0 // The first addition is what we want the rule to be built from.
        )

        app.startRuleSession(SessionStartRequest(id, diffList.selectedChange()))
        val ruleRequest = RuleRequest(id, diffList)
        app.commitRuleSession(ruleRequest)

        withClue("the latest text should be unchanged as the verified text is still null.") {
            interp.latestText() shouldBe "$v1 $v2"
        }

        withClue("The returned DiffList should be updated to reflect the new rule.") {
            interp.diffList shouldBe DiffList(
                diffs = listOf(
                    Unchanged(v1),
                    Addition(v2)
                ),
                selected = -1
            )
        }
    }

    @Test
    fun `should build a rule and return an interpretation containing an updated DiffList when a comment is removed`() {
        val id = "Case1"
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

        val diffList = DiffList(
            diffs = listOf(
                Unchanged(comment1),
                Removal(comment2)
            ),
            selected = 1 // We want the rule to be built for the removal
        )

        app.startRuleSession(SessionStartRequest(id, diffList.selectedChange()))
        val ruleRequest = RuleRequest(id, diffList)
        app.commitRuleSession(ruleRequest)
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

        val diffList = DiffList(
            diffs = listOf(
                Unchanged(comment1),
                Replacement(comment2, comment3)
            ),
            selected = 1 // We want the rule to be built for the removal
        )
        app.startRuleSession(SessionStartRequest(id, diffList.selectedChange()))
        val ruleRequest = RuleRequest(id, diffList)
        app.commitRuleSession(ruleRequest)
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
