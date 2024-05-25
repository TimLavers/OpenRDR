package io.rippledown.main

import io.kotest.matchers.shouldBe
import io.rippledown.mocks.config
import io.rippledown.mocks.mock
import io.rippledown.model.*
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.hasCurrentValue
import io.rippledown.model.diff.*
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
import io.rippledown.model.rule.UpdateCornerstoneRequest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ApiTest {

    @Test
    fun getCaseTest() = runTest {
        val case = createCase("A", 1)
        val config = config {
            returnCase = case
            expectedCaseId = 1
        }
        Api(mock(config)).getCase(1) shouldBe case
    }
    @Test
    fun `should allow the returned case to be null`() = runTest {
        val config = config {
            returnCase = null
        }
        Api(mock(config)).getCase(1) shouldBe null
    }

    @Test
    fun getCaseWithInterpretationTest() = runTest {
        val malabarComment = "go to Malabar"
        val case = createCaseWithInterpretation("A", 1, conclusionTexts = listOf(malabarComment))

        //sanity check
        case.viewableInterpretation.latestText() shouldBe malabarComment

        val config = config {
            returnCase = case
            expectedCaseId = 1
        }

        val retrieved = Api(mock(config)).getCase(1)!!
        retrieved shouldBe case
        retrieved.viewableInterpretation shouldBe case.viewableInterpretation
        retrieved.viewableInterpretation.latestText() shouldBe malabarComment
    }

    @Test
    fun waitingForCasesInfoTest() = runTest {
        val expected = CasesInfo(
            listOf(
                CaseId(1, "case 1"),
                CaseId(2, "case 2"),
            )
        )
        val config = config {
            returnCasesInfo = expected
        }

        Api(mock(config)).waitingCasesInfo() shouldBe expected
    }

    @Test
    fun moveAttributeJustBelowOther() = runTest {
        val expectedResult = OperationResult("Attribute moved.")
        val moved = Attribute(123, "A")
        val target = Attribute(456, "B")
        val config = config {
            returnOperationResult = expectedResult
            expectedMovedAttributeId = moved.id
            expectedTargetAttributeId = target.id
        }
        Api(mock(config)).moveAttribute(moved.id, target.id) shouldBe expectedResult
    }

    @Test
    fun kbInfo() = runTest {
        val config = config {}
        Api(mock(config)).kbInfo().name shouldBe config.defaultKB.name
    }

    @Test
    fun createKB() = runTest {
        val expectedName = "Bondi"
        val config = config { }
        Api(mock(config)).createKB(expectedName)
        config.newKbName shouldBe expectedName
    }

    @Test
    fun kbList() = runTest {
        Api(mock(config {})).kbList().map { it.name } shouldBe listOf("Glucose", "Lipids", "Thyroids")
    }

    @Test
    fun theReturnedInterpretationShouldContainTheDiffList() = runTest {
        val expectedDiffList = DiffList(
            listOf(
                Addition("This comment was added."),
                Removal("This comment was removed."),
                Replacement("This comment was replaced.", "This is the new comment."),
                Unchanged("This comment was left alone."),
            )
        )
        val interpretation = ViewableInterpretation()
        val originalCase = createCase(CaseId())
        val interpToReturn = interpretation.copy(diffList = expectedDiffList)
        val updatedCase = originalCase.copy().apply { viewableInterpretation = interpToReturn }

        val config = config {
            expectedCase = originalCase
            returnCase = updatedCase
        }
        Api(mock(config)).saveVerifiedInterpretation(originalCase) shouldBe updatedCase
    }

    @Test
    fun conditionHints() = runTest {
        val conditionList = ConditionList(
            listOf(
                hasCurrentValue(1, Attribute(1, "A")),
                hasCurrentValue(2, Attribute(2, "B"))
            )
        )
        val config = config {
            returnConditionList = conditionList
        }
        Api(mock(config)).conditionHints(6) shouldBe conditionList
    }

    @Test
    fun shouldBuildRule() = runTest {
        val id = 1L
        val ruleRequest = RuleRequest(
            caseId = id,
            conditions = ConditionList(
                listOf(
                    hasCurrentValue(1, Attribute(1, "A")),
                    hasCurrentValue(2, Attribute(2, "B"))
                )
            )
        )
        val caseToReturn = createCase("A", 1)
        val config = config {
            expectedRuleRequest = ruleRequest
            returnCaseAfterBuildingRule = caseToReturn
        }
        Api(mock(config)).buildRule(ruleRequest) shouldBe caseToReturn
    }

    @Test
    fun shouldStartRuleSession() = runTest {
        val id = 1L
        val sessionStartRequest = SessionStartRequest(
            caseId = id,
            diff = Addition("This comment was added.")
        )

        val config = config {
            expectedSessionStartRequest = sessionStartRequest
            returnCornerstoneStatus = CornerstoneStatus()
        }
        Api(mock(config)).startRuleSession(sessionStartRequest) shouldBe config.returnCornerstoneStatus
    }

    @Test
    fun shouldUpdateCornerstones() = runTest {
        val request = UpdateCornerstoneRequest(
            cornerstoneStatus = CornerstoneStatus(),
            conditionList = ConditionList(
                listOf(
                    hasCurrentValue(1, Attribute(1, "A")),
                    hasCurrentValue(2, Attribute(2, "B"))
                )
            )
        )

        val newCornerstone = createCase("A", 1)
        val config = config {
            expectedUpdateCornerstoneRequest = request
            returnCornerstoneStatus = CornerstoneStatus(newCornerstone, 0, 1)
        }
        Api(mock(config)).updateCornerstoneStatus(request) shouldBe config.returnCornerstoneStatus
    }

    @Test
    fun shouldSelectCornerstone() = runTest {
        val selectedCornerstoneIndex = 42

        val config = config {
            expectedCornerstoneSelection = selectedCornerstoneIndex
            returnCornerstoneStatus = CornerstoneStatus()
        }
        Api(mock(config)).selectCornerstone(selectedCornerstoneIndex) shouldBe config.returnCornerstoneStatus
    }

}
