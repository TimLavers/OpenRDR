package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.model.Attribute
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.RuleConditionList
import io.rippledown.model.condition.lessThanOrEqualTo
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Removal
import io.rippledown.model.diff.Replacement
import io.rippledown.model.rule.*
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.server.websocket.WebSocketManager
import io.rippledown.utils.defaultDate
import kotlin.test.BeforeTest
import kotlin.test.Test

class RuleSessionManagerTest {
    private lateinit var kb: KB
    private lateinit var rsm: RuleSessionManager
    private lateinit var webSocketManager: WebSocketManager

    @BeforeTest
    fun setup() {
        val kbInfo = KBInfo("id123", "TestKB")
        kb = KB(InMemoryKB(kbInfo))
        webSocketManager = mockk()
        rsm = RuleSessionManager(kb, webSocketManager)
    }

    private fun glucose(): Attribute = kb.attributeManager.getOrCreate("Glucose")

    private fun createCase(name: String, value: String = "1.0", id: Long? = null): RDRCase {
        val builder = RDRCaseBuilder()
        builder.addValue(glucose(), defaultDate, value)
        return builder.build(name, id)
    }

    private fun createViewableCase(name: String, value: String = "1.0", id: Long = 1): ViewableCase {
        val case = createCase(name, value, id)
        kb.interpret(case)
        return kb.viewableCase(case)
    }

    // --- startRuleSession ---

    @Test
    fun `should start a rule session for a case`() {
        // Given
        val sessionCase = createCase("Case1")

        // When
        val status =
            rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go.")))

        // Then
        status shouldBe CornerstoneStatus()
        rsm.isRuleSessionActive() shouldBe true
    }

    @Test
    fun `should throw if starting a rule session when one is already active`() {
        // Given
        val sessionCase = createCase("Case1")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go.")))

        // When/Then
        shouldThrow<IllegalStateException> {
            rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Stop.")))
        }.message shouldBe "Session already in progress."
    }

    @Test
    fun `should throw if action is not applicable to the session case`() {
        // Given - add a rule so the case already has the conclusion
        val sessionCase = createCase("Case1", value = "1.0")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go.")))
        rsm.commitCurrentRuleSession()

        val otherCase = createCase("Case2", value = "1.0")
        kb.interpret(otherCase)

        // When/Then
        shouldThrow<IllegalStateException> {
            rsm.startRuleSession(otherCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go.")))
        }
    }

    // --- commitCurrentRuleSession ---

    @Test
    fun `should commit a rule session and add the rule to the tree`() {
        // Given
        val sessionCase = createCase("Case1")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go.")))

        // When
        rsm.commitCurrentRuleSession()

        // Then
        rsm.isRuleSessionActive() shouldBe false
        kb.ruleTree.size() shouldBe 2
        kb.interpret(sessionCase)
        sessionCase.interpretation.conclusionTexts() shouldBe setOf("Go.")
    }

    @Test
    fun `should throw when committing without an active rule session`() {
        // Given - no active session

        // When/Then
        shouldThrow<IllegalStateException> {
            rsm.commitCurrentRuleSession()
        }.message shouldBe "Rule session not started."
    }

    @Test
    fun `should clear currentDiff when committing a rule session`() {
        // Given
        val viewableCase = createViewableCase("Case1", value = "1.0")
        rsm.startRuleSessionToAddComment(viewableCase, "Go.")
        rsm.currentDiff shouldNotBe null

        // When
        rsm.commitCurrentRuleSession()

        // Then
        rsm.currentDiff shouldBe null
    }

    // --- cancelRuleSession ---

    @Test
    fun `should cancel a rule session`() {
        // Given
        val sessionCase = createCase("Case1")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go.")))

        // When
        rsm.cancelRuleSession()

        // Then
        rsm.isRuleSessionActive() shouldBe false
    }

    @Test
    fun `should throw when cancelling without an active rule session`() {
        // Given - no active session

        // When/Then
        shouldThrow<IllegalStateException> {
            rsm.cancelRuleSession()
        }.message shouldBe "No rule session in progress."
    }

    @Test
    fun `should clear currentDiff when cancelling a rule session`() {
        // Given
        val viewableCase = createViewableCase("Case1", value = "1.0")
        rsm.startRuleSessionToAddComment(viewableCase, "Go.")
        rsm.currentDiff shouldNotBe null

        // When
        rsm.cancelRuleSession()

        // Then
        rsm.currentDiff shouldBe null
    }

    // --- addConditionToCurrentRuleSession ---

    @Test
    fun `should add a condition to the current rule session`() {
        // Given
        val sessionCase = createCase("Case1", value = "1.0")
        kb.addCornerstoneCase(createCase("Case2", value = "2.0"))
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go.")))

        // When
        rsm.addConditionToCurrentRuleSession(lessThanOrEqualTo(null, glucose(), 1.2))

        // Then
        rsm.conflictingCasesInCurrentRuleSession().size shouldBe 0
    }

    @Test
    fun `should throw when adding a condition without an active rule session`() {
        // Given - no active session

        // When/Then
        shouldThrow<IllegalStateException> {
            rsm.addConditionToCurrentRuleSession(lessThanOrEqualTo(null, glucose(), 1.2))
        }.message shouldBe "Rule session not started."
    }

    // --- conflictingCasesInCurrentRuleSession ---

    @Test
    fun `should return conflicting cases when a rule session is active`() {
        // Given
        val sessionCase = createCase("Case1", value = "1.0")
        kb.addCornerstoneCase(createCase("Case2", value = "2.0"))
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go.")))

        // When
        val conflicts = rsm.conflictingCasesInCurrentRuleSession()

        // Then
        conflicts.map { it.name }.toSet() shouldBe setOf("Case2")
    }

    @Test
    fun `should throw when getting conflicting cases without an active rule session`() {
        // Given - no active session

        // When/Then
        shouldThrow<IllegalStateException> {
            rsm.conflictingCasesInCurrentRuleSession()
        }.message shouldBe "Rule session not started."
    }

    // --- removeCondition ---

    @Test
    fun `should remove a condition from the current rule session`() {
        // Given
        val sessionCase = createCase("Case1", value = "1.0")
        kb.addCornerstoneCase(createCase("Case2", value = "2.0"))
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go.")))
        val condition = lessThanOrEqualTo(null, glucose(), 1.2)
        rsm.addConditionToCurrentRuleSession(condition)
        rsm.conflictingCasesInCurrentRuleSession().size shouldBe 0

        // When
        val addedCondition = kb.conditionManager.getOrCreate(condition)
        rsm.removeCondition(addedCondition.id!!)

        // Then
        rsm.conflictingCasesInCurrentRuleSession().size shouldBe 1
    }

    // --- startRuleSessionToAddComment ---

    @Test
    fun `should set currentDiff to Addition when starting to add a comment`() {
        // Given
        val viewableCase = createViewableCase("Case1", value = "1.0")
        val comment = "Go to Bondi."

        // When
        rsm.startRuleSessionToAddComment(viewableCase, comment)

        // Then
        rsm.currentDiff shouldBe Addition(comment)
    }

    // --- startRuleSessionToRemoveComment ---

    @Test
    fun `should set currentDiff to Removal when starting to remove a comment`() {
        // Given
        val viewableCase = createViewableCase("Case1", value = "1.0")
        val comment = "Go to Bondi."
        rsm.startRuleSessionToAddComment(viewableCase, comment)
        rsm.commitCurrentRuleSession()

        // When
        rsm.startRuleSessionToRemoveComment(viewableCase, comment)

        // Then
        rsm.currentDiff shouldBe Removal(comment)
    }

    // --- startRuleSessionToReplaceComment ---

    @Test
    fun `should set currentDiff to Replacement when starting to replace a comment`() {
        // Given
        val viewableCase = createViewableCase("Case1", value = "1.0")
        val original = "Go to Bondi."
        val replacement = "Go to Maroubra."
        rsm.startRuleSessionToAddComment(viewableCase, original)
        rsm.commitCurrentRuleSession()

        // When
        rsm.startRuleSessionToReplaceComment(viewableCase, original, replacement)

        // Then
        rsm.currentDiff shouldBe Replacement(original, replacement)
    }

    // --- sendCornerstoneStatus ---

    @Test
    fun `should send cornerstone status via websocket`() {
        // Given
        val sessionCase = createCase("Case1")
        val conclusion = kb.conclusionManager.getOrCreate("Go.")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(conclusion))

        // When
        rsm.sendCornerstoneStatus()

        // Then
        coVerify { webSocketManager.sendStatus(any()) }
    }

    // --- sendRuleSessionCompleted ---

    @Test
    fun `should send rule session completed via websocket`() {
        // Given
        val sessionCase = createCase("Case1")
        val conclusion = kb.conclusionManager.getOrCreate("Go.")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(conclusion))
        rsm.commitCurrentRuleSession()

        // When
        rsm.sendRuleSessionCompleted()

        // Then
        coVerify { webSocketManager.sendRuleSessionCompleted() }
    }

    // --- cornerstoneStatus ---

    @Test
    fun `should return empty cornerstone status when no cornerstones exist`() {
        // Given
        val sessionCase = createCase("Case1")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go.")))

        // When
        val status = rsm.cornerstoneStatus(null)

        // Then
        status shouldBe CornerstoneStatus()
    }

    @Test
    fun `should return cornerstone status with first cornerstone when none selected`() {
        // Given
        val cc1 = kb.addCornerstoneCase(createCase("Case1", value = "1.0"))
        kb.addCornerstoneCase(createCase("Case2", value = "2.0"))
        val vcc1 = kb.viewableCase(cc1)
        val sessionCase = createCase("Case3", value = "3.0")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go.")))

        // When
        val status = rsm.cornerstoneStatus(null)

        // Then
        status shouldBe CornerstoneStatus(vcc1, 0, 2)
    }

    // --- descriptionOfMostRecentRule ---

    @Test
    fun `should return no-undo description when no rules have been built`() {
        // Given - no rules built

        // When
        val description = rsm.descriptionOfMostRecentRule()

        // Then
        description.description shouldBe "There are no rules to undo."
        description.canRemove shouldBe false
    }

    @Test
    fun `should return undo description after building a rule`() {
        // Given
        val sessionCase = createCase("Case1")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go.")))
        rsm.commitCurrentRuleSession()

        // When
        val description = rsm.descriptionOfMostRecentRule()

        // Then
        description.canRemove shouldBe true
    }

    // --- undoLastRuleSession ---

    @Test
    fun `should undo the last rule session`() {
        // Given
        val sessionCase = createCase("Case1", value = "1.0")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go.")))
        rsm.commitCurrentRuleSession()
        val otherCase = createCase("Case2", value = "2.0")
        kb.interpret(otherCase)
        otherCase.interpretation.conclusionTexts() shouldBe setOf("Go.")

        // When
        rsm.undoLastRuleSession()

        // Then
        kb.interpret(otherCase)
        otherCase.interpretation.conclusionTexts() shouldBe emptySet()
    }

    // --- isRuleSessionActive ---

    @Test
    fun `should return false when no rule session is active`() {
        // Given - no active session

        // When/Then
        rsm.isRuleSessionActive() shouldBe false
    }

    @Test
    fun `should return true when a rule session is active`() {
        // Given
        val sessionCase = createCase("Case1")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go.")))

        // When/Then
        rsm.isRuleSessionActive() shouldBe true
    }

    // --- currentRuleSessionConditionTexts ---

    @Test
    fun `should return empty set when no rule session is active for condition texts`() {
        // Given - no active session

        // When/Then
        rsm.currentRuleSessionConditionTexts() shouldBe emptySet()
    }

    @Test
    fun `should return condition texts after adding conditions`() {
        // Given
        val sessionCase = createCase("Case1", value = "1.0")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go.")))
        val condition = lessThanOrEqualTo(null, glucose(), 1.2)

        // When
        rsm.addConditionToCurrentRuleSession(condition)

        // Then
        rsm.currentRuleSessionConditionTexts() shouldBe setOf(condition.asText())
    }

    // --- conditionHintsForCase ---

    @Test
    fun `should return condition hints for a case`() {
        // Given
        val caseWithGlucose = createCase("A", value = "1.0")

        // When
        val hints = rsm.conditionHintsForCase(caseWithGlucose)

        // Then
        hints.suggestions.size shouldNotBe 0
    }

    // --- exemptCornerstone ---

    @Test
    fun `should return empty cornerstone status when the only cornerstone is exempted`() {
        // Given
        val cc1 = kb.addCornerstoneCase(createCase("Case1"))
        val vcc1 = kb.viewableCase(cc1)
        val sessionCase = createCase("Case3")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go.")))

        // When
        val status = rsm.exemptCornerstone(0)

        // Then
        status shouldBe CornerstoneStatus()
    }

    @Test
    fun `should move to next cornerstone after exempting`() {
        // Given
        val cc1 = kb.addCornerstoneCase(createCase("Case1"))
        val cc2 = kb.addCornerstoneCase(createCase("Case2"))
        val vcc2 = kb.viewableCase(cc2)
        val sessionCase = createCase("Case3")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go.")))

        // When
        val status = rsm.exemptCornerstone(0)

        // Then
        status shouldBe CornerstoneStatus(vcc2, 0, 1)
    }

    // --- selectCornerstone ---

    @Test
    fun `should select a specific cornerstone by index`() {
        // Given
        kb.addCornerstoneCase(createCase("Case1", value = "1.0"))
        val cc2 = kb.addCornerstoneCase(createCase("Case2", value = "2.0"))
        val vcc2 = kb.viewableCase(cc2)
        val sessionCase = createCase("Case3", value = "3.0")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go.")))

        // When
        val status = rsm.selectCornerstone(1)

        // Then
        status.cornerstoneToReview shouldNotBe null
        status.indexOfCornerstoneToReview shouldBe 1
        status.numberOfCornerstones shouldBe 2
    }

    // --- ruleSessionHistories ---

    @Test
    fun `should return empty rule session histories when no rules built`() {
        // Given - no rules

        // When
        val histories = rsm.ruleSessionHistories()

        // Then
        histories.size shouldBe 0
    }

    @Test
    fun `should return rule session history after committing a rule`() {
        // Given
        val sessionCase = createCase("Case1")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go.")))
        rsm.commitCurrentRuleSession()

        // When
        val histories = rsm.ruleSessionHistories()

        // Then
        histories.size shouldBe 1
    }

    // --- the session case should be stored as cornerstone ---

    @Test
    fun `should store the session case as a cornerstone when the rule is committed`() {
        // Given
        val sessionCase = createCase("Case1", value = "1.0")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go.")))

        // When
        rsm.commitCurrentRuleSession()

        // Then
        kb.containsCornerstoneCaseWithName("Case1") shouldBe true
    }

    // --- startRuleSession(SessionStartRequest) ---

    @Test
    fun `should start a rule session for an Addition via SessionStartRequest`() {
        // Given
        val storedCase = kb.addProcessedCase(createCase("Case1"))
        val diff = Addition("Go.")
        val request = SessionStartRequest(storedCase.caseId.id!!, diff)

        // When
        rsm.startRuleSession(request)

        // Then
        rsm.isRuleSessionActive() shouldBe true
        rsm.currentDiff shouldBe diff
    }

    @Test
    fun `should start a rule session for a Removal via SessionStartRequest`() {
        // Given
        val storedCase = kb.addProcessedCase(createCase("Case1"))
        val conclusion = kb.conclusionManager.getOrCreate("Go.")
        rsm.startRuleSession(storedCase, ChangeTreeToAddConclusion(conclusion))
        rsm.commitCurrentRuleSession()
        kb.interpret(storedCase)
        val diff = Removal("Go.")
        val request = SessionStartRequest(storedCase.caseId.id!!, diff)

        // When
        rsm.startRuleSession(request)

        // Then
        rsm.isRuleSessionActive() shouldBe true
        rsm.currentDiff shouldBe diff
    }

    @Test
    fun `should start a rule session for a Replacement via SessionStartRequest`() {
        // Given
        val storedCase = kb.addProcessedCase(createCase("Case1"))
        val conclusion = kb.conclusionManager.getOrCreate("Go.")
        rsm.startRuleSession(storedCase, ChangeTreeToAddConclusion(conclusion))
        rsm.commitCurrentRuleSession()
        kb.interpret(storedCase)
        val diff = Replacement("Go.", "Stop.")
        val request = SessionStartRequest(storedCase.caseId.id!!, diff)

        // When
        rsm.startRuleSession(request)

        // Then
        rsm.isRuleSessionActive() shouldBe true
        rsm.currentDiff shouldBe diff
    }

    @Test
    fun `should throw when starting a rule session via SessionStartRequest with unknown case id`() {
        // Given
        val request = SessionStartRequest(9999L, Addition("Go."))

        // When/Then
        shouldThrow<IllegalArgumentException> {
            rsm.startRuleSession(request)
        }.message shouldBe "Case with id 9999 not found"
    }

    // --- commitRuleSession(RuleRequest) ---

    @Test
    fun `should commit a rule session via RuleRequest and return the updated case`() {
        // Given
        val storedCase = kb.addProcessedCase(createCase("Case1", value = "1.0"))
        val caseId = storedCase.caseId.id!!
        kb.interpret(storedCase)
        val diff = Addition("Go.")
        rsm.startRuleSession(SessionStartRequest(caseId, diff))
        val ruleRequest = RuleRequest(caseId)

        // When
        val result = rsm.commitRuleSession(ruleRequest)

        // Then
        rsm.isRuleSessionActive() shouldBe false
        result.viewableInterpretation.interpretation.conclusionTexts() shouldBe setOf("Go.")
    }

    // --- buildRule(BuildRuleRequest) ---

    @Test
    fun `should build a rule via BuildRuleRequest`() {
        // Given
        val glucose = glucose()
        val storedCase = kb.addProcessedCase(createCase("Case1", value = "1.0"))
        kb.interpret(storedCase)
        val request = BuildRuleRequest(
            caseName = "Case1",
            diff = Addition("Glucose ok."),
            conditions = listOf("Glucose ≤ 1.5")
        )

        // When
        rsm.buildRule(request)

        // Then
        val reinterpreted = kb.addProcessedCase(createCase("Case2", value = "1.0"))
        kb.interpret(reinterpreted)
        reinterpreted.interpretation.conclusionTexts() shouldBe setOf("Glucose ok.")
    }

    // --- moveAttributeTo ---

    @Test
    fun `should move an attribute to a new position`() {
        // Given
        val a = kb.attributeManager.getOrCreate("A")
        val b = kb.attributeManager.getOrCreate("B")
        kb.caseViewManager.set(listOf(a, b))
        kb.caseViewManager.allInOrder() shouldBe listOf(a, b)

        // When
        rsm.moveAttributeTo("B", "A")

        // Then
        kb.caseViewManager.allInOrder() shouldBe listOf(b, a)
    }
}
