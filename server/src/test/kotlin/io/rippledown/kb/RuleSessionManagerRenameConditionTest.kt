package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import io.rippledown.model.*
import io.rippledown.model.condition.ConditionText
import io.rippledown.model.condition.greaterThanOrEqualTo
import io.rippledown.model.condition.isHigh
import io.rippledown.model.condition.isLow
import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.CommentTemplate
import io.rippledown.model.rule.Literal
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.server.websocket.WebSocketManager
import io.rippledown.utils.defaultDate
import org.junit.jupiter.api.Test

class RuleSessionManagerRenameConditionTest {
    private val persistence = InMemoryKB(KBInfo("test", "Test"))
    private val kb = KB(persistence)
    private val rsm = RuleSessionManager(kb, mockk<WebSocketManager>())
    private val glucose = kb.attributeManager.getOrCreate("Glucose")
    private val original = kb.conditionManager.getOrCreate(isHigh(null, glucose, "elevated glucose"))
    private val case = kb.addProcessedCase(RDRCaseBuilder().apply {
        addResult(glucose, defaultDate, Result("12.0", ReferenceRange("3", "6")))
    }.build("Case"))

    @Test
    fun `renames by formal text ignoring case and surrounding whitespace`() {
        // Given a stored condition
        // When
        val message = rsm.renameCondition("  GLUCOSE IS HIGH  ", "raised glucose")

        // Then
        message shouldBe "Renamed condition \"Glucose is high\" from \"elevated glucose\" to \"raised glucose\"."
        kb.conditionManager.getById(requireNotNull(original.id)).userExpression() shouldBe "raised glucose"
        original.userExpression() shouldBe "elevated glucose"
        rsm.isRuleSessionActive() shouldBe false
    }

    @Test
    fun `renames by the current phrase and allows repeated renames`() {
        // Given
        rsm.renameCondition("elevated glucose", "  raised glucose  ")

        // When
        val message = rsm.renameCondition(" RAISED GLUCOSE ", "hyperglycaemia")

        // Then
        message shouldBe "Renamed condition \"Glucose is high\" from \"  raised glucose  \" to \"hyperglycaemia\"."
        kb.conditionManager.getOrCreate(isHigh(null, glucose)).userExpression() shouldBe "hyperglycaemia"
    }

    @Test
    fun `names a condition that has no stored phrase`() {
        // Given
        kb.conditionManager.getOrCreate(isLow(null, glucose))

        // When
        val message = rsm.renameCondition("Glucose is low", "low sugar")

        // Then
        message shouldBe "Called condition \"Glucose is low\" \"low sugar\"."
    }

    @Test
    fun `unknown or partial names are refused without changing any phrase`() {
        // Given a stored condition
        // When / Then
        listOf("Glucose", "missing", " ").forEach { text ->
            shouldThrow<IllegalStateException> { rsm.renameCondition(text, "new") }.message shouldBe
                    "No condition \"${text.trim()}\" exists."
        }
        kb.conditionManager.all() shouldBe setOf(original)
    }

    @Test
    fun `ambiguous phrase matches list the conditions and leave them unchanged`() {
        // Given two different predicates with the same phrase
        val other = kb.conditionManager.getOrCreate(isLow(null, glucose).copy(userExpression = "elevated glucose"))

        // When
        val error = shouldThrow<IllegalStateException> { rsm.renameCondition("elevated glucose", "new") }

        // Then
        error.message.orEmpty() shouldContain "Glucose is high"
        error.message.orEmpty() shouldContain "Glucose is low"
        kb.conditionManager.all() shouldBe setOf(original, other)
    }

    @Test
    fun `a formal match that also matches another condition phrase is ambiguous`() {
        // Given
        val other = kb.conditionManager.getOrCreate(isLow(null, glucose).copy(userExpression = "Glucose is high"))

        // When / Then
        shouldThrow<IllegalStateException> { rsm.renameCondition("Glucose is high", "new") }
        kb.conditionManager.all() shouldBe setOf(original, other)
    }

    @Test
    fun `matching both texts of the same condition is not ambiguous`() {
        // Given
        kb.conditionManager.getOrCreate(isLow(null, glucose).copy(userExpression = "Glucose is low"))

        // When
        val message = rsm.renameCondition("Glucose is low", "low sugar")

        // Then
        message shouldBe "Renamed condition \"Glucose is low\" from \"Glucose is low\" to \"low sugar\"."
    }

    @Test
    fun `blank new phrases are refused without changing the stored condition`() {
        // Given a stored condition
        // When / Then
        listOf("", " \t\n").forEach { phrase ->
            shouldThrow<IllegalArgumentException> { rsm.renameCondition("Glucose is high", phrase) }
                .message shouldBe "A condition phrase cannot be blank."
        }
        kb.conditionManager.all() shouldBe setOf(original)
    }

    @Test
    fun `all rules and an active session use the renamed phrase and keep it after reload`() {
        // Given two rules sharing a condition and a third rule being built
        listOf("First", "Second").forEach { text ->
            val attribute = kb.attributeManager.getOrCreate(text, AttributeKind.COMMENT)
            kb.ruleManager.createRuleAndAddToParent(
                kb.ruleTree.root, AssignValue(attribute, CommentTemplate(text)), setOf(original)
            )
        }
        val before = kb.viewableCase(case).viewableInterpretation.renderedComments.map { it.text }
        kb.addCornerstoneCase(case.copy(caseId = CaseId("Cornerstone")))
        rsm.startRuleSessionToAddComment(kb.viewableCase(case), "Third")
        rsm.addConditionToCurrentRuleSession(original)

        // When
        rsm.renameCondition("Glucose is high", "raised glucose")

        // Then current rules and the pending rule show the new phrase, with unchanged inference
        val expected = listOf(ConditionText("Glucose is high", "raised glucose"))
        val comments = kb.viewableCase(case).viewableInterpretation.renderedComments
        comments.map { it.text } shouldBe before
        comments.size shouldBe 2
        comments.forEach { it.conditions shouldBe expected }
        val status = rsm.cornerstoneStatus()
        status.ruleConditions shouldBe expected
        status.numberOfCornerstones shouldBe 1
        requireNotNull(status.cornerstoneToReview).viewableInterpretation.renderedComments
            .forEach { it.conditions shouldBe expected }
        rsm.currentRuleSessionConditionTexts() shouldBe setOf("Glucose is high")

        // When the pending rule is committed and the KB reloaded
        rsm.commitCurrentRuleSession()
        val reloaded = KB(persistence)

        // Then every rule still shows the phrase stored under the original id
        val reloadedComments = reloaded.viewableCase(case).viewableInterpretation.renderedComments
        reloadedComments.size shouldBe 3
        reloadedComments.forEach { it.conditions shouldBe expected }
        reloaded.conditionManager.all().size shouldBe 1
        reloaded.conditionManager.getById(requireNotNull(original.id)).userExpression() shouldBe "raised glucose"
    }

    @Test
    fun `renaming refreshes derived value conditions without changing their values`() {
        // Given a derived value assigned by a condition-bearing rule
        val attribute = kb.attributeManager.getOrCreate("Risk", AttributeKind.DERIVED)
        kb.ruleManager.createRuleAndAddToParent(
            kb.ruleTree.root, AssignValue(attribute, Literal("high")), setOf(original)
        )

        // When
        rsm.renameCondition("elevated glucose", "raised glucose")

        // Then
        val info = kb.viewableCase(case).derivedValues().single()
        info.value shouldBe "high"
        info.conditions shouldBe listOf(ConditionText("Glucose is high", "raised glucose"))
    }

    @Test
    fun `renaming an unused condition keeps the pending rule conditions unchanged`() {
        // Given a session using another stored condition
        val unrelated = kb.conditionManager.getOrCreate(greaterThanOrEqualTo(null, glucose, 1.0))
        rsm.startRuleSessionToAddComment(kb.viewableCase(case), "Advice")
        rsm.addConditionToCurrentRuleSession(unrelated)

        // When
        rsm.renameCondition("elevated glucose", "raised glucose")

        // Then
        rsm.cornerstoneStatus().ruleConditions shouldBe listOf(ConditionText.of(unrelated))
        rsm.isRuleSessionActive() shouldBe true
    }
}
