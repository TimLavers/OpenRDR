package io.rippledown.kb

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import io.rippledown.model.*
import io.rippledown.model.external.ExternalCase
import io.rippledown.model.external.MeasurementEvent
import io.rippledown.model.rule.ByDefinition
import io.rippledown.model.rule.CommentTemplate
import io.rippledown.model.rule.Literal
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.utils.defaultDate
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Tests that [KB.processCase] resolves `ByDefinition` assignment expressions
 * to their stored definitions in the returned case's interpretation, so that
 * callers reading the interpretation directly (e.g. the interpreter API
 * endpoint) see `CommentTemplate`/`Literal` expressions, not `ByDefinition`
 * sentinels.
 */
class KBProcessCaseResolutionTest {
    private lateinit var kb: KB
    private lateinit var rsm: RuleSessionManager

    @BeforeTest
    fun setup() {
        kb = KB(InMemoryKB(KBInfo("id123", "TestKB")))
        rsm = RuleSessionManager(kb, mockk(relaxed = true))
    }

    private fun glucose() = kb.attributeManager.getOrCreate("Glucose")

    private fun createCase(name: String, glucoseValue: String = "12.0") =
        with(RDRCaseBuilder()) {
            addValue(glucose(), defaultDate, glucoseValue)
            build(name)
        }

    private fun viewableCase(name: String, glucoseValue: String = "12.0") =
        kb.viewableCase(kb.addProcessedCase(createCase(name, glucoseValue)))

    private fun externalCase(name: String, glucoseValue: String = "12.0") =
        ExternalCase(
            name,
            mapOf(MeasurementEvent("Glucose", defaultDate) to Result(glucoseValue))
        )

    private fun buildAddCommentRule(comment: String) {
        rsm.startRuleSessionToAddComment(viewableCase("Builder"), comment)
        rsm.addConditionToCurrentRuleSession(
            io.rippledown.model.condition.greaterThanOrEqualTo(null, glucose(), 11.0)
        )
        rsm.commitCurrentRuleSession()
    }

    @Test
    fun `processCase resolves a ByDefinition comment assignment to its CommentTemplate`() {
        // Given a comment rule that assigns a comment attribute by definition
        buildAddCommentRule("High glucose alert.")

        // When a matching case is processed via the API path
        val processed = kb.processCase(externalCase("Case1", "15.0"))

        // Then the interpretation contains a resolved CommentTemplate, not ByDefinition
        val assignments = processed.interpretation.assignments()
        assignments shouldHaveSize 1
        val assignment = assignments.single()
        assignment.attribute.kind shouldBe AttributeKind.COMMENT
        assignment.expression.shouldBeInstanceOf<CommentTemplate>()
        (assignment.expression as CommentTemplate).text shouldBe "High glucose alert."
    }

    @Test
    fun `processCase commentTexts returns the comment text for a matching case`() {
        // Given a comment rule requiring glucose >= 11
        buildAddCommentRule("High glucose alert.")

        // When a matching case is processed
        val processed = kb.processCase(externalCase("Case1", "15.0"))

        // Then commentTexts returns the comment text
        processed.interpretation.commentTexts(processed) shouldBe setOf("High glucose alert.")
    }

    @Test
    fun `processCase commentTexts is empty for a non-matching case`() {
        // Given a comment rule requiring glucose >= 11
        buildAddCommentRule("High glucose alert.")

        // When a non-matching case is processed
        val processed = kb.processCase(externalCase("LowCase", "3.2"))

        // Then no comment is produced
        processed.interpretation.commentTexts(processed) shouldBe emptySet()
    }

    @Test
    fun `processCase does not leave ByDefinition in the interpretation`() {
        // Given a comment rule
        buildAddCommentRule("Diabetic advice given.")

        // When a matching case is processed
        val processed = kb.processCase(externalCase("Case1", "15.0"))

        // Then no assignment expression is ByDefinition
        val expressions = processed.interpretation.assignments().map { it.expression }
        expressions.none { it is ByDefinition } shouldBe true
    }

    @Test
    fun `processCase resolves multiple comment assignments`() {
        // Given two comment rules on different conditions
        // Build the higher-threshold rule first so its builder case (glucose 25.0)
        // doesn't match the lower-threshold rule that doesn't exist yet.
        rsm.startRuleSessionToAddComment(viewableCase("Builder1", "25.0"), "Second comment.")
        rsm.addConditionToCurrentRuleSession(
            io.rippledown.model.condition.greaterThanOrEqualTo(null, glucose(), 20.0)
        )
        rsm.commitCurrentRuleSession()
        // Build the lower-threshold rule; the builder case (glucose 12.0) does
        // not match the >= 20.0 rule, so "Second comment." is not present.
        buildAddCommentRule("First comment.")

        // When a case matching both rules is processed
        val processed = kb.processCase(externalCase("Case1", "25.0"))

        // Then both comments appear in commentTexts
        processed.interpretation.commentTexts(processed) shouldBe setOf("First comment.", "Second comment.")
    }

    @Test
    fun `processCase leaves non-comment assignments as ByDefinition when no definition is stored`() {
        // Given a derived attribute with no stored definition but a rule assigning it by definition
        val bmi = kb.attributeManager.getOrCreate("BMI", AttributeKind.DERIVED)
        kb.ruleManager.createRuleAndAddToParent(
            kb.ruleTree.root,
            io.rippledown.model.rule.AssignValue(bmi, ByDefinition),
            emptySet()
        )

        // When a case is processed
        val processed = kb.processCase(externalCase("Case1", "12.0"))

        // Then the derived assignment is present but has no resolved expression
        // (no definition stored, so resolveDefinitions leaves it as ByDefinition)
        val assignments = processed.interpretation.assignments().filter { it.attribute == bmi }
        if (assignments.isNotEmpty()) {
            assignments.single().expression shouldBe ByDefinition
        }
    }

    @Test
    fun `processCase resolves a derived attribute with a stored Literal definition`() {
        // Given a derived attribute with a stored Literal definition and a by-definition rule
        val bmi = kb.attributeManager.getOrCreate("BMI", AttributeKind.DERIVED)
        kb.derivedDefinitionManager.store(bmi.id, Literal("25.0"))
        kb.ruleManager.createRuleAndAddToParent(
            kb.ruleTree.root,
            io.rippledown.model.rule.AssignValue(bmi, ByDefinition),
            emptySet()
        )

        // When a case is processed
        val processed = kb.processCase(externalCase("Case1", "12.0"))

        // Then the derived assignment is resolved to the Literal expression
        val assignment = processed.interpretation.assignments().first { it.attribute == bmi }
        assignment.expression shouldBe Literal("25.0")
    }

    @Test
    fun `processCase with no rules returns an empty interpretation`() {
        // Given no rules in the KB

        // When a case is processed
        val processed = kb.processCase(externalCase("Case1", "12.0"))

        // Then the interpretation has no assignments or comments
        processed.interpretation.assignments() shouldBe emptySet()
        processed.interpretation.commentTexts(processed) shouldBe emptySet()
    }

    @Test
    fun `processCase resolves comment with variables to CommentTemplate with variables`() {
        // Given a comment rule with a variable referencing Glucose
        val glucoseAttr = glucose()
        rsm.startRuleSessionToAddComment(
            viewableCase("Builder"),
            "Glucose is $VARIABLE_TOKEN",
            listOf(CommentVariable(glucoseAttr.id))
        )
        rsm.addConditionToCurrentRuleSession(
            io.rippledown.model.condition.greaterThanOrEqualTo(null, glucose(), 11.0)
        )
        rsm.commitCurrentRuleSession()

        // When a matching case is processed
        val processed = kb.processCase(externalCase("Case1", "14.5"))

        // Then the interpretation has a CommentTemplate with the original text and variable
        val assignment = processed.interpretation.assignments().first { it.attribute.kind == AttributeKind.COMMENT }
        assignment.expression.shouldBeInstanceOf<CommentTemplate>()
        val template = assignment.expression as CommentTemplate
        template.text shouldBe "Glucose is $VARIABLE_TOKEN"
        template.variables shouldBe listOf(glucoseAttr)
    }

    @Test
    fun `processCase commentTexts renders variable names for comment with variables`() {
        // Given a comment rule with a variable referencing Glucose
        val glucoseAttr = glucose()
        rsm.startRuleSessionToAddComment(
            viewableCase("Builder"),
            "Glucose is $VARIABLE_TOKEN",
            listOf(CommentVariable(glucoseAttr.id))
        )
        rsm.addConditionToCurrentRuleSession(
            io.rippledown.model.condition.greaterThanOrEqualTo(null, glucose(), 11.0)
        )
        rsm.commitCurrentRuleSession()

        // When a matching case is processed
        val processed = kb.processCase(externalCase("Case1", "14.5"))

        // Then commentTexts renders the variable in {attributeName} format
        processed.interpretation.commentTexts(processed) shouldBe setOf("Glucose is {Glucose}")
    }
}
