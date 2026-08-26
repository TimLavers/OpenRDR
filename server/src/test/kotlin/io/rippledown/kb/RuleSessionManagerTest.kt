package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.rippledown.model.*
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.isCondition
import io.rippledown.model.condition.isPresent
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
            rsm.startRuleSessionToAssignComment(kb, sessionCase, "Go.")

        // Then
        status shouldBe CornerstoneStatus()
        rsm.isRuleSessionActive() shouldBe true
    }

    @Test
    fun `should throw if starting a rule session when one is already active`() {
        // Given
        val sessionCase = createCase("Case1")
        rsm.startRuleSessionToAddComment(sessionCase, "Go.")

        // When/Then
        shouldThrow<IllegalStateException> {
            rsm.startRuleSessionToAddComment(sessionCase, "Stop.")
        }.message shouldBe "Session already in progress."
    }

    @Test
    fun `should throw if action is not applicable to the session case`() {
        // Given - add a rule so the case already has the comment
        val sessionCase = createCase("Case1", value = "1.0")
        rsm.startRuleSessionToAddComment(sessionCase, "Go.")
        rsm.commitCurrentRuleSession()

        val otherCase = createCase("Case2", value = "1.0")
        kb.interpret(otherCase)

        // When/Then
        shouldThrow<IllegalStateException> {
            rsm.startRuleSessionToAddComment(otherCase, "Go.")
        }
    }

    // --- commitCurrentRuleSession ---

    @Test
    fun `should commit a rule session and add the rule to the tree`() {
        // Given
        val sessionCase = createCase("Case1")
        rsm.startRuleSessionToAddComment(sessionCase, "Go.")

        // When
        rsm.commitCurrentRuleSession()

        // Then
        rsm.isRuleSessionActive() shouldBe false
        kb.ruleTree.size() shouldBe 2
        kb.interpret(sessionCase)
        kb.commentsFor(sessionCase) shouldBe setOf("Go.")
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
        rsm.startRuleSessionToAddComment(viewableCase, "Go.", emptyList())
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
        rsm.startRuleSessionToAddComment(sessionCase, "Go.")

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
        rsm.startRuleSessionToAddComment(viewableCase, "Go.", emptyList())
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
        rsm.startRuleSessionToAddComment(sessionCase, "Go.")

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
        rsm.startRuleSessionToAddComment(sessionCase, "Go.")

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
        rsm.startRuleSessionToAddComment(sessionCase, "Go.")
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
        rsm.startRuleSessionToAddComment(viewableCase, comment, emptyList())

        // Then
        rsm.currentDiff shouldBe Addition(comment, "C1")
    }

    @Test
    fun `should show the variable name, not its value, in the diff when adding a comment with a variable`() {
        // Given
        val viewableCase = createViewableCase("Case1", value = "5.0")
        val template = "Glucose is " + io.rippledown.model.VARIABLE_TOKEN
        val variables = listOf(CommentVariable(glucose().id))

        // When
        rsm.startRuleSessionToAddComment(viewableCase, template, variables)

        // Then - the pending comment shows the template, with the variable named
        rsm.currentDiff shouldBe Addition("Glucose is {Glucose}", "C1")
    }

    /**
     * A comment variable naming a *derived* attribute must not be evaluated in the
     * preview either. The chat holds the case it was given when the conversation
     * started, so a derived attribute whose rule was built later in the same
     * conversation is absent from that snapshot, and rendering against it produced
     * "{BMI: no value}". A pending comment is a template, so the variable name is
     * shown regardless of whether a value is available.
     */
    @Test
    fun `should show the variable name for a derived attribute that is absent from the supplied case`() {
        // Given a committed rule assigning the derived attribute BMI
        val bmi = kb.attributeManager.getOrCreate("BMI", AttributeKind.DERIVED)
        rsm.startRuleSessionToAssignValue(createCase("Setup", value = "5.0"), "BMI", "Glucose * 2")
        rsm.commitCurrentRuleSession()

        // And a case snapshot taken before that rule existed, so it carries no
        // value for BMI - exactly what the chat's start-of-conversation case is
        val staleCase = createCase("Case1", value = "5.0")
        staleCase.latestValue(bmi).shouldBeNull()
        val viewableCase = ViewableCase(staleCase, CaseViewProperties(listOf(glucose())))

        // When a comment referencing BMI is previewed
        val template = "BMI is " + io.rippledown.model.VARIABLE_TOKEN
        rsm.startRuleSessionToAddComment(viewableCase, template, listOf(CommentVariable(bmi.id)))

        // Then the variable is shown by name, not marked as missing
        rsm.currentDiff shouldBe Addition("BMI is {BMI}", "C1")
    }

    // --- startRuleSessionToRemoveComment ---

    @Test
    fun `should set currentDiff to Removal when starting to remove a comment`() {
        // Given
        val viewableCase = createViewableCase("Case1", value = "1.0")
        val comment = "Go to Bondi."
        rsm.startRuleSessionToAddComment(viewableCase, comment, emptyList())
        rsm.commitCurrentRuleSession()

        // When
        rsm.startRuleSessionToRemoveComment(viewableCase, comment)

        // Then
        rsm.currentDiff shouldBe Removal(comment, "C1")
    }

    @Test
    fun `should resolve the removed comment to its existing comment when it contains a variable`() {
        // Given a committed comment that itself contains a variable. Its stored comment holds the
        // placeholder as VARIABLE_TOKEN with a CommentVariable, so the comment to remove must be matched
        // to that existing comment rather than a fresh, variable-less one (which would not appear in
        // the case's interpretation and so could not be removed).
        val viewableCase = createViewableCase("Case1", value = "5.0")
        val template = "Glucose is " + io.rippledown.model.VARIABLE_TOKEN
        val variables = listOf(CommentVariable(glucose().id))
        rsm.startRuleSessionToAddComment(viewableCase, template, variables)
        rsm.commitCurrentRuleSession()

        // When the same (internal-form) comment is removed
        rsm.startRuleSessionToRemoveComment(viewableCase, template)

        // Then the existing comment is matched (so the action is applicable) and the diff shows the
        // comment as its rule defines it, with the variable named.
        rsm.currentDiff shouldBe Removal("Glucose is {Glucose}", "C1")
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
        // The replacing comment is a new comment attribute, so it is auto-named C2.
        rsm.currentDiff shouldBe Replacement(original, replacement, "C2", "C1")
    }

    @Test
    fun `should show the variable name, not its value, in the diff when replacing a comment with a variable`() {
        // Given
        val viewableCase = createViewableCase("Case1", value = "5.0")
        val original = "Old comment"
        val template = "Glucose is " + io.rippledown.model.VARIABLE_TOKEN
        val variables = listOf(CommentVariable(glucose().id))
        rsm.startRuleSessionToAddComment(viewableCase, original)
        rsm.commitCurrentRuleSession()

        // When
        rsm.startRuleSessionToReplaceComment(viewableCase, original, template, variables)

        // Then - the pending replacement shows the template, with the variable named
        rsm.currentDiff shouldBe Replacement(original, "Glucose is {Glucose}", "C2", "C1")
    }

    @Test
    fun `should resolve the replaced comment to its existing comment when it contains a variable`() {
        // Given a committed comment that itself contains a variable. Its stored comment holds the
        // placeholder as VARIABLE_TOKEN with a CommentVariable, so the replaced comment must be matched
        // to that existing comment rather than a fresh, variable-less one (which would not appear in
        // the case's interpretation and would make the replace action inapplicable).
        val viewableCase = createViewableCase("Case1", value = "5.0")
        val template = "Glucose is " + io.rippledown.model.VARIABLE_TOKEN
        val variables = listOf(CommentVariable(glucose().id))
        rsm.startRuleSessionToAddComment(viewableCase, template, variables)
        rsm.commitCurrentRuleSession()

        val replacement = "Go to Maroubra."

        // When the same (internal-form) comment is replaced
        rsm.startRuleSessionToReplaceComment(viewableCase, template, replacement)

        // Then the existing comment is matched (so the action is applicable) and the diff shows the
        // comment being replaced as its rule defines it, with the variable named.
        rsm.currentDiff shouldBe Replacement("Glucose is {Glucose}", replacement, "C2", "C1")
    }

    // --- attributeForName ---

    @Test
    fun `attributeForName should find exact match case-insensitive`() {
        // Given
        kb.attributeManager.getOrCreate("Glucose")

        // When
        val result = rsm.attributeForName("glucose")

        // Then
        result?.name shouldBe "Glucose"
    }

    @Test
    fun `attributeForName should find exact match with different case`() {
        // Given
        kb.attributeManager.getOrCreate("Glucose")

        // When
        val result = rsm.attributeForName("GLUCOSE")

        // Then
        result?.name shouldBe "Glucose"
    }

    @Test
    fun `attributeForName should return null for non-existent attribute`() {
        // Given
        kb.attributeManager.getOrCreate("Glucose")

        // When
        val result = rsm.attributeForName("NonExistent")

        // Then
        result shouldBe null
    }

    @Test
    fun `attributeForName should find match with normalized punctuation`() {
        // Given
        kb.attributeManager.getOrCreate("TSH (free)")

        // When
        val result = rsm.attributeForName("TSH free")

        // Then
        result?.name shouldBe "TSH (free)"
    }

    @Test
    fun `attributeForName should find match with small misspelling via Levenshtein`() {
        // Given
        kb.attributeManager.getOrCreate("Glucose")

        // When
        val result = rsm.attributeForName("Gluose")

        // Then
        result?.name shouldBe "Glucose"
    }

    @Test
    fun `attributeForName should reject large misspellings`() {
        // Given
        kb.attributeManager.getOrCreate("Glucose")

        // When
        val result = rsm.attributeForName("Xyz")

        // Then
        result shouldBe null
    }

    // --- allAttributes ---

    @Test
    fun `allAttributes should return all attributes in the knowledge base`() {
        // Given
        kb.attributeManager.getOrCreate("Glucose")
        kb.attributeManager.getOrCreate("Haemoglobin")

        // When
        val result = rsm.allAttributes()

        // Then
        result.map { it.name } shouldContainAll listOf("Glucose", "Haemoglobin")
    }

    @Test
    fun `allAttributes should return an empty set when the knowledge base has no attributes`() {
        // When
        val result = rsm.allAttributes()

        // Then
        result shouldBe emptySet()
    }

    // --- sendCornerstoneStatus ---

    @Test
    fun `should send cornerstone status via websocket`() {
        // Given
        val sessionCase = createCase("Case1")
        rsm.startRuleSessionToAddComment(sessionCase, "Go.")

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
        rsm.startRuleSessionToAddComment(sessionCase, "Go.")
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
        rsm.startRuleSessionToAssignComment(kb, sessionCase, "Go.")

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
        rsm.startRuleSessionToAssignComment(kb, sessionCase, "Go.")

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
        rsm.startRuleSessionToAddComment(sessionCase, "Go.")
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
        rsm.startRuleSessionToAddComment(sessionCase, "Go.")
        rsm.commitCurrentRuleSession()
        val otherCase = createCase("Case2", value = "2.0")
        kb.interpret(otherCase)
        kb.commentsFor(otherCase) shouldBe setOf("Go.")

        // When
        rsm.undoLastRuleSession()

        // Then
        kb.interpret(otherCase)
        kb.commentsFor(otherCase) shouldBe emptySet()
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
        rsm.startRuleSessionToAddComment(sessionCase, "Go.")

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
        rsm.startRuleSessionToAddComment(sessionCase, "Go.")
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
        rsm.startRuleSessionToAddComment(sessionCase, "Go.")

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
        rsm.startRuleSessionToAssignComment(kb, sessionCase, "Go.")

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
        rsm.startRuleSessionToAddComment(sessionCase, "Go.")

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
        rsm.startRuleSessionToAddComment(sessionCase, "Go.")
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
        rsm.startRuleSessionToAddComment(sessionCase, "Go.")

        // When
        rsm.commitCurrentRuleSession()

        // Then
        kb.containsCornerstoneCaseWithName("Case1") shouldBe true
    }

    // --- commit should push updated CasesInfo over the websocket ---

    @Test
    fun `should send updated CasesInfo via websocket when a rule session is committed`() {
        // Given
        val sessionCase = createCase("Case1", value = "1.0")
        rsm.startRuleSessionToAddComment(sessionCase, "Go.")

        // When
        rsm.commitCurrentRuleSession()

        // Then
        coVerify { webSocketManager.sendCasesInfo(any()) }
    }

    @Test
    fun `sent CasesInfo should include the newly added cornerstone case`() {
        // Given
        val sessionCase = createCase("Bondi", value = "1.0")
        rsm.startRuleSessionToAddComment(sessionCase, "Go.")
        val capturedCasesInfo = slot<CasesInfo>()

        // When
        rsm.commitCurrentRuleSession()

        // Then
        coVerify { webSocketManager.sendCasesInfo(capture(capturedCasesInfo)) }
        capturedCasesInfo.captured.cornerstoneCaseIds.map { it.name } shouldBe listOf("Bondi")
    }

    @Test
    fun `sent CasesInfo should include existing cornerstones plus the newly added one`() {
        // Given
        kb.addCornerstoneCase(createCase("Existing1", value = "1.0"))
        kb.addCornerstoneCase(createCase("Existing2", value = "2.0"))
        val sessionCase = createCase("Bondi", value = "3.0")
        rsm.startRuleSessionToAddComment(sessionCase, "Go.")
        val capturedCasesInfo = slot<CasesInfo>()

        // When
        rsm.commitCurrentRuleSession()

        // Then
        coVerify { webSocketManager.sendCasesInfo(capture(capturedCasesInfo)) }
        capturedCasesInfo.captured.cornerstoneCaseIds.map { it.name } shouldBe
                listOf("Existing1", "Existing2", "Bondi")
    }

    @Test
    fun `sent CasesInfo should include processed case ids`() {
        // Given
        val processed1 = kb.addProcessedCase(createCase("Processed1", value = "1.0"))
        val processed2 = kb.addProcessedCase(createCase("Processed2", value = "2.0"))
        kb.interpret(processed1)
        val diff = Addition("Go.")
        rsm.startRuleSession(SessionStartRequest(processed1.caseId.id!!, diff))
        val capturedCasesInfo = slot<CasesInfo>()

        // When
        rsm.commitCurrentRuleSession()

        // Then
        coVerify { webSocketManager.sendCasesInfo(capture(capturedCasesInfo)) }
        capturedCasesInfo.captured.caseIds.map { it.name } shouldBe
                listOf(processed1.name, processed2.name)
    }

    @Test
    fun `sent CasesInfo should carry the KB name`() {
        // Given
        val sessionCase = createCase("Case1", value = "1.0")
        rsm.startRuleSessionToAddComment(sessionCase, "Go.")
        val capturedCasesInfo = slot<CasesInfo>()

        // When
        rsm.commitCurrentRuleSession()

        // Then
        coVerify { webSocketManager.sendCasesInfo(capture(capturedCasesInfo)) }
        capturedCasesInfo.captured.kbName shouldBe kb.kbInfo.name
    }

    @Test
    fun `CasesInfo should be sent exactly once per commit`() {
        // Given
        val sessionCase = createCase("Case1", value = "1.0")
        rsm.startRuleSessionToAddComment(sessionCase, "Go.")

        // When
        rsm.commitCurrentRuleSession()

        // Then
        coVerify(exactly = 1) { webSocketManager.sendCasesInfo(any()) }
    }

    @Test
    fun `multiple commits should each push a CasesInfo reflecting the accumulated cornerstones`() {
        // Given
        val case1 = createCase("Case1", value = "1.0")
        rsm.startRuleSessionToAddComment(case1, "Go.")
        rsm.commitCurrentRuleSession()

        val case2 = createCase("Case2", value = "2.0")
        kb.interpret(case2)
        rsm.startRuleSessionToAddComment(case2, "Stop.")
        val capturedCasesInfo = mutableListOf<CasesInfo>()

        // When
        rsm.commitCurrentRuleSession()

        // Then
        coVerify(exactly = 2) { webSocketManager.sendCasesInfo(capture(capturedCasesInfo)) }
        capturedCasesInfo[0].cornerstoneCaseIds.map { it.name } shouldBe listOf("Case1")
        capturedCasesInfo[1].cornerstoneCaseIds.map { it.name } shouldBe listOf("Case1", "Case2")
    }

    @Test
    fun `CasesInfo should not be sent when commit fails because no session is active`() {
        // Given - no active session

        // When
        shouldThrow<IllegalStateException> {
            rsm.commitCurrentRuleSession()
        }

        // Then
        coVerify(exactly = 0) { webSocketManager.sendCasesInfo(any()) }
    }

    @Test
    fun `commit via RuleRequest should also push updated CasesInfo`() {
        // Given
        val storedCase = kb.addProcessedCase(createCase("Case1", value = "1.0"))
        val caseId = storedCase.caseId.id!!
        kb.interpret(storedCase)
        rsm.startRuleSession(SessionStartRequest(caseId, Addition("Go.")))
        val capturedCasesInfo = slot<CasesInfo>()

        // When
        rsm.commitRuleSession(RuleRequest(caseId))

        // Then
        coVerify { webSocketManager.sendCasesInfo(capture(capturedCasesInfo)) }
        capturedCasesInfo.captured.cornerstoneCaseIds.map { it.name } shouldBe listOf("Case1")
    }

    @Test
    fun `buildRule should push updated CasesInfo via the websocket`() {
        // Given
        val storedCase = kb.addProcessedCase(createCase("Case1", value = "1.0"))
        kb.interpret(storedCase)
        val request = BuildRuleRequest(
            caseName = "Case1",
            diff = Addition("Glucose ok."),
            conditions = listOf("Glucose ≤ 1.5")
        )
        val capturedCasesInfo = slot<CasesInfo>()

        // When
        rsm.buildRule(request)

        // Then
        coVerify { webSocketManager.sendCasesInfo(capture(capturedCasesInfo)) }
        capturedCasesInfo.captured.cornerstoneCaseIds.map { it.name } shouldBe listOf("Case1")
    }

    @Test
    fun `commit should not throw when webSocketManager is null`() {
        // Given
        val rsmWithoutWs = RuleSessionManager(kb, null)
        val sessionCase = createCase("Case1", value = "1.0")
        rsmWithoutWs.startRuleSessionToAddComment(sessionCase, "Go.")

        // When / Then - no exception thrown and cornerstone still persisted
        rsmWithoutWs.commitCurrentRuleSession()
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

        // Then the recorded change names the comment attribute the server minted
        rsm.isRuleSessionActive() shouldBe true
        rsm.currentDiff shouldBe diff.copy(attributeName = "C1")
    }

    @Test
    fun `should start a rule session for a Removal via SessionStartRequest`() {
        // Given
        val storedCase = kb.addProcessedCase(createCase("Case1"))
        rsm.startRuleSessionToAddComment(storedCase, "Go.")
        rsm.commitCurrentRuleSession()
        kb.interpret(storedCase)
        val diff = Removal("Go.")
        val request = SessionStartRequest(storedCase.caseId.id!!, diff)

        // When
        rsm.startRuleSession(request)

        // Then
        rsm.isRuleSessionActive() shouldBe true
        rsm.currentDiff shouldBe diff.copy(attributeName = "C1")
    }

    @Test
    fun `should start a rule session for a Replacement via SessionStartRequest`() {
        // Given
        val storedCase = kb.addProcessedCase(createCase("Case1"))
        rsm.startRuleSessionToAddComment(storedCase, "Go.")
        rsm.commitCurrentRuleSession()
        kb.interpret(storedCase)
        val diff = Replacement("Go.", "Stop.")
        val request = SessionStartRequest(storedCase.caseId.id!!, diff)

        // When
        rsm.startRuleSession(request)

        // Then the replacing comment is a new comment attribute, so it is auto-named C2,
        // and the change names the attribute being replaced
        rsm.isRuleSessionActive() shouldBe true
        rsm.currentDiff shouldBe diff.copy(attributeName = "C2", replacedAttributeName = "C1")
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
        result.viewableInterpretation.renderedComments.map { it.text } shouldBe listOf("Go.")
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
        kb.viewableCase(reinterpreted).viewableInterpretation
            .renderedComments.map { it.text } shouldBe listOf("Glucose ok.")
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

    // --- cornerstone case creation

    @Test
    fun `add session case as cornerstone`() {
        val sessionCase1 = createCase("Case1", value = "1.0")
        rsm.startRuleSessionToAddComment(sessionCase1, "C1")
        rsm.commitCurrentRuleSession()
        kb.cornerstoneCaseIds().size shouldBe 1

        val case2 = createCase("Case2", value = "1.1")
        rsm.startRuleSessionToAddComment(case2, "C2")
        rsm.commitCurrentRuleSession()
        kb.cornerstoneCaseIds().size shouldBe 2

        val case3 = createCase("Case3", value = "1.0")
        rsm.startRuleSessionToAddComment(case3, "C3")
        rsm.commitCurrentRuleSession()
        kb.cornerstoneCaseIds().size shouldBe 2  // Case3 is identical to Case1 so is not stored
    }

    // --- cycles through comment attributes

    @Test
    fun `should refuse a condition that would make a comment attribute depend on itself`() {
        // Given a rule that gives a comment
        val case = createCase("Case1", value = "12.0")
        rsm.startRuleSessionToAddComment(case, "Advice.")
        rsm.commitCurrentRuleSession()
        val comment = kb.attributeManager.commentAttributes().single()

        // And a rule assigning a derived value, conditioned on that comment
        kb.interpret(case)
        val status = kb.attributeManager.getOrCreate("Status", AttributeKind.DERIVED)
        rsm.startRuleSession(case, ChangeTreeToAddAssignment(AssignValue(status, Literal("high"))))
        rsm.addConditionToCurrentRuleSession(isPresent(comment))
        rsm.commitCurrentRuleSession()

        // When a rule retracting the comment is conditioned on that derived value,
        // the comment would depend on itself, so interpretation would never settle
        kb.interpret(case)
        rsm.startRuleSessionToRemoveComment(case, "Advice.")

        // Then the condition is refused
        shouldThrow<IllegalArgumentException> {
            rsm.addConditionToCurrentRuleSession(isCondition(null, status, "high"))
        }.message shouldContain "depend on itself"
    }

    // --- copyCaseToFavourites ---

    @Test
    fun `should copy a processed case to favourites`() {
        // Given
        val stored = kb.addProcessedCase(createCase("Case1", value = "1.0"))
        val viewableCase = kb.viewableCase(stored)

        // When
        val copy = rsm.copyCaseToFavourites(viewableCase, null)

        // Then
        copy.name shouldBe "Case1"
        copy.caseId.type shouldBe CaseType.Favourite
        copy.id shouldNotBe stored.id
        copy.data shouldBe stored.data
    }

    @Test
    fun `should copy a case to favourites with a new name`() {
        // Given
        val stored = kb.addProcessedCase(createCase("Case1", value = "1.0"))
        val viewableCase = kb.viewableCase(stored)

        // When
        val copy = rsm.copyCaseToFavourites(viewableCase, "My favourite")

        // Then
        copy.name shouldBe "My favourite"
        copy.caseId.type shouldBe CaseType.Favourite
        copy.id shouldNotBe stored.id
        copy.data shouldBe stored.data
    }

    @Test
    fun `should copy a cornerstone case to favourites`() {
        // Given - the current case can be copied whether it is from the
        // Processed list, the Cornerstone list, or the Favourites list itself
        val stored = kb.addCornerstoneCase(createCase("Case1", value = "1.0"))
        val viewableCase = kb.viewableCase(stored)

        // When
        val copy = rsm.copyCaseToFavourites(viewableCase, null)

        // Then
        copy.caseId.type shouldBe CaseType.Favourite
        copy.data shouldBe stored.data
    }

    @Test
    fun `should copy a favourite case to favourites again`() {
        // Given
        val original = kb.addProcessedCase(createCase("Case1", value = "1.0"))
        val favourite = kb.copyCaseAsFavourite(original.id!!, null)
        val viewableFavourite = kb.viewableCase(favourite)

        // When
        val copyOfCopy = rsm.copyCaseToFavourites(viewableFavourite, null)

        // Then
        copyOfCopy.caseId.type shouldBe CaseType.Favourite
        copyOfCopy.id shouldNotBe favourite.id
        copyOfCopy.data shouldBe favourite.data
    }

    @Test
    fun `should add the copy to the favourites list`() {
        // Given
        val stored = kb.addProcessedCase(createCase("Case1", value = "1.0"))
        val viewableCase = kb.viewableCase(stored)

        // When
        val copy = rsm.copyCaseToFavourites(viewableCase, null)

        // Then
        kb.favouriteCaseIds().map { it.id } shouldBe listOf(copy.id)
    }

    @Test
    fun `should not remove the original case from its own list when copying it to favourites`() {
        // Given
        val stored = kb.addProcessedCase(createCase("Case1", value = "1.0"))
        val viewableCase = kb.viewableCase(stored)

        // When
        rsm.copyCaseToFavourites(viewableCase, null)

        // Then
        kb.processedCaseIds().map { it.id } shouldBe listOf(stored.id)
    }

    @Test
    fun `should send updated CasesInfo via websocket when a case is copied to favourites`() {
        // Given
        val stored = kb.addProcessedCase(createCase("Case1", value = "1.0"))
        val viewableCase = kb.viewableCase(stored)
        val capturedCasesInfo = slot<CasesInfo>()

        // When
        val copy = rsm.copyCaseToFavourites(viewableCase, null)

        // Then
        coVerify { webSocketManager.sendCasesInfo(capture(capturedCasesInfo)) }
        capturedCasesInfo.captured.favouriteCaseIds.map { it.id } shouldBe listOf(copy.id)
    }

    // --- deleteCaseFromFavourites ---

    @Test
    fun `should delete a case from favourites`() {
        // Given
        val stored = kb.addProcessedCase(createCase("Case1", value = "1.0"))
        val favourite = kb.copyCaseAsFavourite(stored.id!!, null)
        val viewableFavourite = kb.viewableCase(favourite)

        // When
        rsm.deleteCaseFromFavourites(viewableFavourite)

        // Then
        kb.favouriteCaseIds() shouldBe emptyList()
    }

    @Test
    fun `should not delete the original case when deleting its favourite copy`() {
        // Given
        val stored = kb.addProcessedCase(createCase("Case1", value = "1.0"))
        val favourite = kb.copyCaseAsFavourite(stored.id!!, null)
        val viewableFavourite = kb.viewableCase(favourite)

        // When
        rsm.deleteCaseFromFavourites(viewableFavourite)

        // Then
        kb.processedCaseIds().map { it.id } shouldBe listOf(stored.id)
    }

    @Test
    fun `should leave other favourites untouched when deleting one favourite`() {
        // Given
        val stored = kb.addProcessedCase(createCase("Case1", value = "1.0"))
        val toDelete = kb.copyCaseAsFavourite(stored.id!!, "To delete")
        val toKeep = kb.copyCaseAsFavourite(stored.id!!, "To keep")
        val viewableToDelete = kb.viewableCase(toDelete)

        // When
        rsm.deleteCaseFromFavourites(viewableToDelete)

        // Then
        kb.favouriteCaseIds().map { it.id } shouldBe listOf(toKeep.id)
    }

    @Test
    fun `should throw when deleting a case that is not a favourite`() {
        // Given
        val stored = kb.addProcessedCase(createCase("Case1", value = "1.0"))
        val viewableCase = kb.viewableCase(stored)

        // When/Then
        shouldThrow<IllegalArgumentException> {
            rsm.deleteCaseFromFavourites(viewableCase)
        }.message shouldBe "Case is not a favourite"
    }

    @Test
    fun `should send updated CasesInfo via websocket when a case is deleted from favourites`() {
        // Given
        val stored = kb.addProcessedCase(createCase("Case1", value = "1.0"))
        val favourite = kb.copyCaseAsFavourite(stored.id!!, null)
        val viewableFavourite = kb.viewableCase(favourite)

        // When
        rsm.deleteCaseFromFavourites(viewableFavourite)

        // Then
        coVerify { webSocketManager.sendCasesInfo(any()) }
    }

    @Test
    fun `sent CasesInfo should no longer include the deleted favourite`() {
        // Given
        val stored = kb.addProcessedCase(createCase("Case1", value = "1.0"))
        val deleted = kb.copyCaseAsFavourite(stored.id!!, "Deleted")
        val kept = kb.copyCaseAsFavourite(stored.id!!, "Kept")
        val viewableDeleted = kb.viewableCase(deleted)
        val capturedCasesInfo = slot<CasesInfo>()

        // When
        rsm.deleteCaseFromFavourites(viewableDeleted)

        // Then
        coVerify { webSocketManager.sendCasesInfo(capture(capturedCasesInfo)) }
        capturedCasesInfo.captured.favouriteCaseIds.map { it.id } shouldBe listOf(kept.id)
    }
}
