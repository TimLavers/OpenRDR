package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.rippledown.constants.chat.renamedMessage
import io.rippledown.model.*
import io.rippledown.model.change.ChangeKind
import io.rippledown.model.condition.isHigh
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.server.websocket.WebSocketManager
import io.rippledown.utils.defaultDate
import kotlin.test.BeforeTest
import kotlin.test.Test

class RuleSessionManagerChangeLogTest {
    private lateinit var kb: KB
    private lateinit var rsm: RuleSessionManager
    private lateinit var webSocketManager: WebSocketManager
    private val alice = UserId("alice")

    @BeforeTest
    fun setup() {
        kb = KB(InMemoryKB(KBInfo("id123", "TestKB")))
        webSocketManager = mockk()
        rsm = RuleSessionManager(kb, webSocketManager, userId = alice)
    }

    private fun glucose(): Attribute = kb.attributeManager.getOrCreate("Glucose")

    private fun caseWithHighGlucose(name: String) = kb.addProcessedCase(RDRCaseBuilder().apply {
        addResult(glucose(), defaultDate, Result("12.0", ReferenceRange("3", "6")))
    }.build(name))

    private fun commitRuleAddingComment(case: RDRCase, comment: String) {
        rsm.startRuleSessionToAddComment(case, comment)
        rsm.addConditionToCurrentRuleSession(isHigh(null, glucose()))
        rsm.commitCurrentRuleSession()
    }

    @Test
    fun `committing a rule session records the rules added`() {
        // Given
        val case = caseWithHighGlucose("Case1")

        // When
        commitRuleAddingComment(case, "Go to the beach.")

        // Then
        val record = kb.changeLog.all().single()
        val rule = kb.ruleTree.root.childRules().single()
        record.userId shouldBe "alice"
        record.kind shouldBe ChangeKind.RULES_ADDED
        record.summary shouldBe "Added comment \"Go to the beach.\" when Glucose is high."
        record.ruleIds shouldBe setOf(rule.id)
        record.attributeIds shouldBe setOf(requireNotNull(rule.assignment).attribute.id)
    }

    @Test
    fun `a refused commit records nothing`() {
        // Given a session whose case's interpretation changes before the commit
        val case = caseWithHighGlucose("Case1")
        rsm.startRuleSessionToAddComment(case, "Go to the beach.")
        val other = RuleSessionManager(kb, webSocketManager, userId = UserId("bob"))
        other.startRuleSessionToAddComment(case, "Go to the beach.")
        other.commitCurrentRuleSession()
        kb.changeLog.all().size shouldBe 1

        // When
        shouldThrow<StaleRuleSessionException> { rsm.commitCurrentRuleSession() }

        // Then
        kb.changeLog.all().size shouldBe 1
    }

    @Test
    fun `undoing a rule session records the undo with the ids of the rules removed`() {
        // Given
        val case = caseWithHighGlucose("Case1")
        commitRuleAddingComment(case, "Go to the beach.")
        val ruleId = kb.ruleTree.root.childRules().single().id
        val commentAttributeId = kb.changeLog.all().single().attributeIds.single()

        // When
        rsm.undoLastRuleSession()

        // Then
        val record = kb.changeLog.all().last()
        record.kind shouldBe ChangeKind.RULES_UNDONE
        record.summary shouldBe "Undid: Added comment \"Go to the beach.\" when Glucose is high."
        record.ruleIds shouldBe setOf(ruleId)
        record.attributeIds shouldBe setOf(commentAttributeId)
    }

    @Test
    fun `editing a derived attribute's definition is recorded`() {
        // Given
        val bmi = kb.attributeManager.getOrCreate("BMI", AttributeKind.DERIVED)

        // When
        val message = rsm.editDerivedAttributeDefinition("BMI", "42")

        // Then
        val record = kb.changeLog.all().single()
        record.kind shouldBe ChangeKind.DEFINITION_EDITED
        record.summary shouldBe message
        record.attributeIds shouldBe setOf(bmi.id)
        record.ruleIds.shouldBeEmpty()
    }

    @Test
    fun `renaming an attribute is recorded`() {
        // Given
        val comment = kb.attributeManager.createCommentAttribute()
        val oldName = comment.name

        // When
        rsm.renameAttribute(oldName, "Beach advice")

        // Then
        val record = kb.changeLog.all().single()
        record.kind shouldBe ChangeKind.ATTRIBUTE_RENAMED
        record.summary shouldBe renamedMessage(oldName, "Beach advice")
        record.attributeIds shouldBe setOf(comment.id)
    }

    @Test
    fun `renaming a condition is recorded`() {
        // Given
        kb.conditionManager.getOrCreate(isHigh(null, glucose(), "elevated glucose"))

        // When
        val message = rsm.renameCondition("Glucose is high", "sugar up")

        // Then
        val record = kb.changeLog.all().single()
        record.kind shouldBe ChangeKind.CONDITION_RENAMED
        record.summary shouldBe message
        record.attributeIds.shouldBeEmpty()
    }

    @Test
    fun `moving an attribute is recorded`() {
        // Given
        val glucose = glucose()
        val tsh = kb.attributeManager.getOrCreate("TSH")
        kb.caseViewManager.set(listOf(glucose, tsh))

        // When
        rsm.moveAttributeTo("Glucose", "TSH")

        // Then
        val record = kb.changeLog.all().single()
        record.kind shouldBe ChangeKind.ATTRIBUTE_MOVED
        record.summary shouldBe attributeMovedSummary(glucose, tsh)
        record.attributeIds shouldBe setOf(glucose.id, tsh.id)
    }

    @Test
    fun `deleting a case from a user list is recorded`() {
        // Given
        val case = caseWithHighGlucose("Case1")
        val copied = kb.copyCaseToList(requireNotNull(case.id), "Good ones", "Keeper")

        // When
        rsm.deleteCaseFromUserList(kb.viewableCase(copied))

        // Then
        val record = kb.changeLog.all().single()
        record.kind shouldBe ChangeKind.CASE_DELETED
        record.summary shouldBe "Deleted case \"Keeper\" from the \"Good ones\" list."
    }

    @Test
    fun `a manager with no user records nothing`() {
        // Given
        val userless = RuleSessionManager(kb, webSocketManager)
        val case = caseWithHighGlucose("Case1")

        // When
        userless.startRuleSessionToAddComment(case, "Go to the beach.")
        userless.commitCurrentRuleSession()

        // Then
        kb.ruleTree.size() shouldBe 2
        kb.changeLog.all().shouldBeEmpty()
    }
}
