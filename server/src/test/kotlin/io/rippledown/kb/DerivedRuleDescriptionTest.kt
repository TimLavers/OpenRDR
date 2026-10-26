package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.constants.chat.KB_CHANGED_PREFIX
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.UserId
import io.rippledown.model.change.ChangeKind
import io.rippledown.model.change.KbChange
import io.rippledown.model.condition.lessThanOrEqualTo
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.server.websocket.WebSocketManager
import io.rippledown.toJsonString
import io.rippledown.utils.defaultDate
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class DerivedRuleDescriptionTest {
    private val kb = KB(InMemoryKB(KBInfo("derived", "Derived descriptions")))
    private val webSocketManager = mockk<WebSocketManager>()
    private val session = KBSession(kb, webSocketManager)
    private val alice = session.ruleSessionManagerFor(UserId("Alice"))
    private val bob = session.ruleSessionManagerFor(UserId("Bob"))
    private val x = kb.attributeManager.getOrCreate("x")

    private fun case(name: String, value: String = "1") = RDRCaseBuilder().apply {
        addValue(x, defaultDate, value)
    }.build(name)

    private fun commitDefinition(expression: String = "2 * x") = session.locked {
        alice.startRuleSessionToAssignValue(case("Case1"), "x2", expression)
        alice.addConditionToCurrentRuleSession(lessThanOrEqualTo(null, x, 2.0))
        alice.commitCurrentRuleSession()
    }

    @ParameterizedTest
    @ValueSource(strings = ["2 * x", "2 * (x + 1)", "\"a literal value\""])
    fun `history and broadcast show the stored expression rather than by definition`(expression: String) {
        // Given a user building a rule with a staged definition
        session.locked {
            alice.startRuleSessionToAssignValue(case("Case1"), "x2", expression)
            alice.addConditionToCurrentRuleSession(lessThanOrEqualTo(null, x, 2.0))
        }

        // When the rule and its definition are committed
        session.locked { alice.commitCurrentRuleSession() }

        // Then history and the notice broadcast to other users include the expression, unevaluated
        val record = kb.changeLog.all().single()
        record.userId shouldBe "Alice"
        record.kind shouldBe ChangeKind.RULES_ADDED
        record.summary shouldBe "Assigned x2 = $expression when x ≤ 2.0."
        verify(exactly = 1) {
            webSocketManager.broadcast(KB_CHANGED_PREFIX + KbChange(kb.kbInfo.id, record).toJsonString())
        }
    }

    @Test
    fun `replacing an assignment describes both the definition and the concrete override`() {
        // Given a rule assigning x2 by definition
        commitDefinition()

        // When a more specific rule overrides it with a different expression
        session.locked {
            bob.startRuleSessionToReplaceAssignment(case("Case2", "0"), "x2", "3 * x")
            bob.addConditionToCurrentRuleSession(lessThanOrEqualTo(null, x, 0.5))
            bob.commitCurrentRuleSession()
        }

        // Then the original definition and the override are both shown
        kb.changeLog.all().last().summary shouldBe
                "Replaced x2 = 2 * x with x2 = 3 * x when x ≤ 0.5."
        bob.descriptionOfMostRecentRule().description shouldBe
                "Replace assignment:\nx2 = 2 * x\nwith:\nx2 = 3 * x"
    }

    @Test
    fun `retracting an assignment describes the definition being removed`() {
        // Given a rule assigning x2 by definition
        commitDefinition()

        // When a more specific rule retracts that assignment
        session.locked {
            bob.startRuleSessionToRemoveAssignment(case("Case2", "0"), "x2")
            bob.addConditionToCurrentRuleSession(lessThanOrEqualTo(null, x, 0.5))
            bob.commitCurrentRuleSession()
        }

        // Then the removal names the expression it retracts
        kb.changeLog.all().last().summary shouldBe "Retracted x2 = 2 * x when x ≤ 0.5."
        bob.descriptionOfMostRecentRule().description shouldBe "Retract assignment:\nx2 = 2 * x"
    }

    @Test
    fun `undo preview and history show the expression and retain authorship`() {
        // Given Alice has committed a rule assigning x2 by definition
        commitDefinition()

        // When Bob previews and undoes Alice's rule
        val preview = bob.descriptionOfMostRecentRule()
        bob.undoLastRuleSession()

        // Then both the preview and undo history include the expression
        preview.canRemove shouldBe true
        preview.description shouldBe "The most recent rule was added by Alice: Assign value:\nx2 = 2 * x"
        val record = kb.changeLog.all().last()
        record.kind shouldBe ChangeKind.RULES_UNDONE
        record.userId shouldBe "Bob"
        record.summary shouldBe "Undid: Assigned x2 = 2 * x when x ≤ 2.0."
    }

    @Test
    fun `editing a definition preserves the expression in earlier history`() {
        // Given an existing change record for a rule using the original definition
        commitDefinition()

        // When the definition is edited
        session.locked { bob.editDerivedAttributeDefinition("x2", "3 * x") }

        // Then history retains the expression at commit time while the undo preview shows the current rule
        kb.changeLog.all().first().summary shouldBe "Assigned x2 = 2 * x when x ≤ 2.0."
        alice.descriptionOfMostRecentRule().description shouldBe "Assign value:\nx2 = 3 * x"
    }
}
