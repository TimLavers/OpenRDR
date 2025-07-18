package io.rippledown.main

import io.kotest.matchers.shouldBe
import io.rippledown.mocks.config
import io.rippledown.mocks.mock
import io.rippledown.model.*
import io.rippledown.model.condition.*
import io.rippledown.model.condition.edit.NonEditableSuggestedCondition
import io.rippledown.model.diff.Addition
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
import io.rippledown.model.rule.UpdateCornerstoneRequest
import io.rippledown.sample.SampleKB.TSH_CASES
import io.rippledown.utils.createViewableCase
import io.rippledown.utils.createViewableCaseWithInterpretation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ApiTest {
    val case = createViewableCase("A", 1)

    @Test
    fun kbDescriptionTest() = runTest {
        val config = config {}
        Api(mock(config)).kbDescription() shouldBe config.returnedKbDescription
    }

    @Test
    fun kbDescriptionPostTest() = runTest {
        val descriptionText = """
            #OpenRDR:
            A comprehensive description is given in
            ["Ripple-Down Rules, the Alternative to Machine Learning"](https://www.amazon.com.au/Ripple-Down-Rules-Alternative-Machine-Learning-ebook/dp/B092KVD3HQ)
            """.trimIndent()
        val config = config {}
        Api(mock(config)).setKbDescription(descriptionText)
        config.returnedKbDescription shouldBe descriptionText
    }

    @Test
    fun getCaseTest() = runTest {
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
        //Given
        val malabarComment = "go to Malabar"
        val case = createViewableCaseWithInterpretation("A", 1, conclusionTexts = listOf(malabarComment))

        //sanity check
        case.viewableInterpretation.latestText() shouldBe malabarComment

        val config = config {
            returnCase = case
            expectedCaseId = 1
        }
        //When
        val retrieved = Api(mock(config)).getCase(1)!!

        //Then
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
    fun `should retrieve all conclusions for the specified kb`() = runTest {
        val expected = setOf(
            Conclusion(1, "A"),
            Conclusion(2, "B")
        )

        val config = config {
            returnConclusions = expected
        }

        Api(mock(config)).allConclusions() shouldBe expected
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
    fun createKBFromSample() = runTest {
        val expectedName = "Bondi"
        val config = config { }
        val result = Api(mock(config)).createKBFromSample(expectedName, TSH_CASES)
        config.newKbName shouldBe expectedName
        config.sampleKB shouldBe TSH_CASES
        result.name shouldBe expectedName
    }

    @Test
    fun kbList() = runTest {
        Api(mock(config {})).kbList().map { it.name } shouldBe listOf("Glucose", "Lipids", "Thyroids")
    }

    @Test
    fun conditionHints() = runTest {
        val conditionList = conditionList(
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

    private fun conditionList(conditions: List<Condition>) =
        ConditionList(conditions.map { NonEditableSuggestedCondition(it) })

    private fun ruleConditionList(conditions: List<Condition>) = RuleConditionList(conditions)

    @Test
    fun shouldBuildRule() = runTest {
        val id = 1L
        val ruleRequest = RuleRequest(
            caseId = id,
            conditions = ruleConditionList(
                listOf(
                    hasCurrentValue(1, Attribute(1, "A")),
                    hasCurrentValue(2, Attribute(2, "B"))
                )
            )
        )
        val caseToReturn = createViewableCase("A", 1)
        val config = config {
            expectedRuleRequest = ruleRequest
            returnCaseAfterBuildingRule = caseToReturn
        }
        Api(mock(config)).commitSession(ruleRequest) shouldBe caseToReturn
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
    fun `should cancel the current rule session`() = runTest {
        val config = config {
            expectedSessionCancel = true
        }
        Api(mock(config)).cancelRuleSession()
    }

    @Test
    fun shouldUpdateCornerstones() = runTest {
        val request = UpdateCornerstoneRequest(
            cornerstoneStatus = CornerstoneStatus(),
            conditionList = ruleConditionList(
                listOf(
                    hasCurrentValue(1, Attribute(1, "A")),
                    hasCurrentValue(2, Attribute(2, "B"))
                )
            )
        )

        val newCornerstone = createViewableCase("A", 1)
        val config = config {
            expectedUpdateCornerstoneRequest = request
            returnCornerstoneStatus = CornerstoneStatus(newCornerstone, 0, 1)
        }
        Api(mock(config)).updateCornerstoneStatus(request) shouldBe config.returnCornerstoneStatus
    }

    @Test
    fun `should exempt a cornerstone case`() = runTest {
        val updatedCornerstoneStatus = CornerstoneStatus(createViewableCase("Bondi"), 42, 100)

        val config = config {
            expectedCornerstoneIndex = 42
            returnCornerstoneStatus = updatedCornerstoneStatus
        }
        Api(mock(config)).exemptCornerstone(42) shouldBe config.returnCornerstoneStatus
    }

    @Test
    fun shouldSelectCornerstone() = runTest {
        val updatedCornerstoneStatus = CornerstoneStatus(createViewableCase("Bondi"), 42, 100)

        val config = config {
            expectedCornerstoneIndex = 42
            returnCornerstoneStatus = updatedCornerstoneStatus
        }
        Api(mock(config)).selectCornerstone(42) shouldBe config.returnCornerstoneStatus
    }

    @Test
    fun `should return a condition for the specified expression`() = runTest {
        val config = config {
            expectedExpression = "Great surf"
            expectedAttributeNames = listOf("Surf", "Sun")
            val condition = hasCurrentValue(1, Attribute(1, "Surf"))
            returnConditionParsingResult = ConditionParsingResult(condition)
        }
        val returned =
            Api(mock(config)).conditionFor(config.expectedExpression)
        returned shouldBe config.returnConditionParsingResult
    }

    @Test
    fun `should return an error message for the specified expression`() = runTest {
        val config = config {
            expectedExpression = "Great surf"
            expectedAttributeNames = listOf("Surf", "Sun")
            returnConditionParsingResult = ConditionParsingResult(errorMessage = "unknown expression")
        }
        val returned =
            Api(mock(config)).conditionFor(config.expectedExpression)
        returned shouldBe config.returnConditionParsingResult
    }

    @Test
    fun `should return a response from the conversation with the model`() = runTest {
        val userMessage = "What is the meaning of life?"
        val caseId = 1234L
        val config = config {
            expectedUserMessage = userMessage
            expectedCaseId = caseId
            returnResponse = "42"
        }
        val response = Api(mock(config)).sendUserMessage(userMessage, caseId)
        response shouldBe config.returnResponse
    }

    @Test
    fun `should start a conversation with the model`() = runTest {
        val caseId = 1234L
        val config = config {
            expectedCaseId = caseId
            returnResponse = "42"
        }
        val response = Api(mock(config)).startConversation(caseId)
        response shouldBe config.returnResponse
    }

    @Test
    fun `should return description of most recent rule`() = runTest {
        val config = config {}
        with(Api(mock(config)).lastRuleDescription()) {
            this.description shouldBe "It was a great rule, but it has to go."
            this.canRemove shouldBe true
        }
    }

    @Test
    fun `undo the last rule`() = runTest {
        val config = config {}
        config.lastRuleUndoCalled shouldBe false
        Api(mock(config)).undoLastRule()
        config.lastRuleUndoCalled shouldBe true
    }
}
