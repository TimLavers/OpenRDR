package io.rippledown.kb.chat

import io.kotest.matchers.shouldBe
import io.mockk.*
import io.rippledown.kb.KBSession
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.UndoRuleDescription
import kotlin.test.BeforeTest
import kotlin.test.Test

class LockedRuleServiceTest {
    private lateinit var session: KBSession
    private lateinit var delegate: RuleService
    private lateinit var service: LockedRuleService
    private val viewableCase = mockk<ViewableCase>()
    private val case = mockk<RDRCase>()
    private val condition = mockk<Condition>()
    private val status = CornerstoneStatus()

    @BeforeTest
    fun setup() {
        session = mockk()
        every { session.locked(any<() -> Any?>()) } answers { firstArg<() -> Any?>()() }
        delegate = mockk()
        service = LockedRuleService(session, delegate)
    }

    private fun stubEditingCalls() {
        every { delegate.startRuleSessionToAddComment(viewableCase, "Go.", emptyList()) } returns status
        every { delegate.startRuleSessionToRemoveComment(viewableCase, "Go.") } returns status
        every { delegate.startRuleSessionToReplaceComment(viewableCase, "Go.", "Stop.", emptyList()) } returns status
        every { delegate.startRuleSessionToAssignValue(viewableCase, "Ratio", "A / B") } returns status
        every { delegate.startRuleSessionToRemoveAssignment(viewableCase, "Ratio") } returns status
        every { delegate.startRuleSessionToReplaceAssignment(viewableCase, "Ratio", "A * B") } returns status
        every { delegate.renameAttribute("C1", "Advice") } returns "Renamed."
        every { delegate.renameCondition("x > 1", "x is high") } returns "Renamed."
        every { delegate.editDerivedAttributeDefinition("Ratio", "A * B") } returns "Edited."
        every { delegate.exemptCornerstoneCase() } returns status
        every { delegate.selectCornerstoneCase(2) } returns status
        every { delegate.addConditionToCurrentRuleSession(condition) } just Runs
        every { delegate.commitCurrentRuleSession() } just Runs
        every { delegate.cancelCurrentRuleSession() } just Runs
        every { delegate.undoLastRuleSession() } just Runs
        every { delegate.moveAttributeTo("A", "B") } just Runs
        every { delegate.removeCondition(3) } returns status
        every { delegate.removeConditionByText("x > 1") } returns status
        every { delegate.copyCaseToList(viewableCase, "Favourites", null) } returns case
        every { delegate.deleteCaseFromUserList(viewableCase) } just Runs
    }

    private val editingCalls: List<Pair<String, RuleService.() -> Any?>> = listOf(
        "startRuleSessionToAddComment" to { startRuleSessionToAddComment(viewableCase, "Go.") },
        "startRuleSessionToRemoveComment" to { startRuleSessionToRemoveComment(viewableCase, "Go.") },
        "startRuleSessionToReplaceComment" to { startRuleSessionToReplaceComment(viewableCase, "Go.", "Stop.") },
        "startRuleSessionToAssignValue" to { startRuleSessionToAssignValue(viewableCase, "Ratio", "A / B") },
        "startRuleSessionToRemoveAssignment" to { startRuleSessionToRemoveAssignment(viewableCase, "Ratio") },
        "startRuleSessionToReplaceAssignment" to {
            startRuleSessionToReplaceAssignment(
                viewableCase,
                "Ratio",
                "A * B"
            )
        },
        "renameAttribute" to { renameAttribute("C1", "Advice") },
        "renameCondition" to { renameCondition("x > 1", "x is high") },
        "editDerivedAttributeDefinition" to { editDerivedAttributeDefinition("Ratio", "A * B") },
        "exemptCornerstoneCase" to { exemptCornerstoneCase() },
        "selectCornerstoneCase" to { selectCornerstoneCase(2) },
        "addConditionToCurrentRuleSession" to { addConditionToCurrentRuleSession(condition) },
        "commitCurrentRuleSession" to { commitCurrentRuleSession() },
        "cancelCurrentRuleSession" to { cancelCurrentRuleSession() },
        "undoLastRuleSession" to { undoLastRuleSession() },
        "moveAttributeTo" to { moveAttributeTo("A", "B") },
        "removeCondition" to { removeCondition(3) },
        "removeConditionByText" to { removeConditionByText("x > 1") },
        "copyCaseToList" to { copyCaseToList(viewableCase, "Favourites") },
        "deleteCaseFromUserList" to { deleteCaseFromUserList(viewableCase) },
    )

    @Test
    fun `each editing call runs under the KB lock and delegates`() {
        // Given
        stubEditingCalls()

        // When
        editingCalls.forEach { (_, call) -> service.call() }

        // Then
        verify(exactly = editingCalls.size) { session.locked(any<() -> Any?>()) }
        verify(exactly = 1) { delegate.startRuleSessionToAddComment(viewableCase, "Go.", emptyList()) }
        verify(exactly = 1) { delegate.commitCurrentRuleSession() }
        verify(exactly = 1) { delegate.deleteCaseFromUserList(viewableCase) }
    }

    @Test
    fun `reads and pushes run under the KB lock`() {
        // Given
        every { delegate.nameOfCommentAttributeInSession() } returns "C1"
        every { delegate.offeredValueExpressionFor("x") } returns null
        every { delegate.conditionForExpression(case, "x > 1") } returns ConditionParsingResult(condition)
        every { delegate.descriptionOfMostRecentRule() } returns UndoRuleDescription("Rule.", false)
        every { delegate.sendCornerstoneStatus() } just Runs
        every { delegate.sendRuleSessionCompleted() } just Runs
        every { delegate.cornerstoneStatus() } returns status
        every { delegate.conditionHintsForCase(case) } returns ConditionList()
        every { delegate.conditionForSuggestionText(case, "x > 1") } returns condition
        every { delegate.currentRuleSessionConditionTexts() } returns setOf("x > 1")
        every { delegate.isRuleSessionActive() } returns true
        every { delegate.attributeForName("x") } returns Attribute(1, "x")
        every { delegate.attributeById(1) } returns Attribute(1, "x")
        every { delegate.allAttributes() } returns setOf(Attribute(1, "x"))

        // When
        service.nameOfCommentAttributeInSession() shouldBe "C1"
        service.offeredValueExpressionFor("x") shouldBe null
        service.conditionForExpression(case, "x > 1").condition shouldBe condition
        service.descriptionOfMostRecentRule() shouldBe UndoRuleDescription("Rule.", false)
        service.sendCornerstoneStatus()
        service.sendRuleSessionCompleted()
        service.cornerstoneStatus() shouldBe status
        service.conditionHintsForCase(case) shouldBe ConditionList()
        service.conditionForSuggestionText(case, "x > 1") shouldBe condition
        service.currentRuleSessionConditionTexts() shouldBe setOf("x > 1")
        service.isRuleSessionActive() shouldBe true
        service.attributeForName("x") shouldBe Attribute(1, "x")
        service.attributeById(1) shouldBe Attribute(1, "x")
        service.allAttributes() shouldBe setOf(Attribute(1, "x"))

        // Then every call but the LLM-translating one ran under the lock
        verify(exactly = 13) { session.locked(any<() -> Any?>()) }
    }

    @Test
    fun `translating a condition expression is not wrapped in the KB lock`() {
        // Given
        every { delegate.conditionForExpression(case, "x > 1") } returns ConditionParsingResult(condition)

        // When
        service.conditionForExpression(case, "x > 1")

        // Then the delegate holds the lock only around its own KB access, so none is taken here
        verify(exactly = 0) { session.locked(any<() -> Any?>()) }
    }
}
