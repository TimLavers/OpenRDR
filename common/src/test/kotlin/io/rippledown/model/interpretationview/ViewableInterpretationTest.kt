package io.rippledown.model.interpretationview

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.CaseId
import io.rippledown.model.Interpretation
import io.rippledown.model.condition.containsText
import io.rippledown.model.condition.isCondition
import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.CommentTemplate
import io.rippledown.model.rule.Literal
import io.rippledown.model.rule.Rule
import io.rippledown.utils.checkSerializationIsThreadSafe
import io.rippledown.utils.serializeDeserialize
import kotlin.test.BeforeTest
import kotlin.test.Test

class ViewableInterpretationTest {
    private val caseId = CaseId()
    private var attributeId = 0
    private var conditionId = 0
    private val c1 = Attribute(100, "C1", AttributeKind.COMMENT)
    private val c2 = Attribute(101, "C2", AttributeKind.COMMENT)
    private lateinit var interp: Interpretation

    @BeforeTest
    fun init() {
        interp = Interpretation(caseId)
    }

    private fun comment(attribute: Attribute, text: String) = AssignValue(attribute, CommentTemplate(text))

    @Test
    fun constructorWithNoViewFields() {
        with(ViewableInterpretation(interp)) {
            interpretation shouldBe interp
            latestText() shouldBe ""
            textGivenByRules shouldBe ""
        }
    }

    @Test
    fun `the text given by rules is that supplied by the knowledge base`() {
        // The text is computed by the server, which resolves each comment
        // attribute to its definition, rather than by the view itself.
        val rule = Rule(0, null, emptySet(), mutableSetOf(), comment(c1, "First comment"))
        interp.add(rule)
        ViewableInterpretation(interp, textGivenByRules = "First comment").textGivenByRules shouldBe "First comment"
    }

    @Test
    fun serialisationWithInterpretation() {
        val assignment = comment(c1, "First comment")
        val conditions = setOf(
            isCondition(1, Attribute(1, "x"), "1"),
        )
        val rule = Rule(0, null, conditions, mutableSetOf(), assignment)
        interp.apply { add(rule) }
        val view = ViewableInterpretation(interp, textGivenByRules = "First comment")
        val restored = serializeDeserialize(view)
        restored.interpretation shouldBe interp
        restored.latestText() shouldBe "First comment"

        checkSerializationIsThreadSafe(interp)
    }

    @Test
    fun serialisationWithRuleSummary() {
        val assignment = comment(c1, "First comment")
        val conditions = setOf(
            isCondition(1, Attribute(1, "x"), "1"),
        )
        val ruleSummary = Rule(0, null, conditions, mutableSetOf(), assignment).summary()
        interp.apply { add(ruleSummary) }
        val view = ViewableInterpretation(interp, textGivenByRules = "First comment")
        withClue("sanity check") {
            view.textGivenByRules shouldBe "First comment"
        }
        val restored = serializeDeserialize(view)
        restored.interpretation shouldBe interp
        restored.textGivenByRules shouldBe "First comment"

        checkSerializationIsThreadSafe(interp)
    }

    @Test
    fun `assignments returns the set of AssignValue actions from the interpretation`() {
        // Given an interpretation with two assignment rules
        val attr1 = Attribute(attributeId++, "Alpha", AttributeKind.DERIVED)
        val attr2 = Attribute(attributeId++, "Beta", AttributeKind.DERIVED)
        val assignment1 = AssignValue(attr1, Literal("yes"))
        val assignment2 = AssignValue(attr2, Literal("no"))
        val rule1 = Rule(1, null, emptySet(), mutableSetOf(), assignment1)
        val rule2 = Rule(2, null, emptySet(), mutableSetOf(), assignment2)
        interp.add(rule1)
        interp.add(rule2)
        val view = ViewableInterpretation(interp)

        // When asking for assignments
        val result = view.assignments()

        // Then both assignments are returned
        result shouldBe setOf(assignment1, assignment2)
    }

    @Test
    fun `conditionsForAssignment returns condition texts for an assignment`() {
        // Given an interpretation with an assignment rule
        val external = Attribute(attributeId++, "Glucose", AttributeKind.EXTERNAL)
        val derived = Attribute(attributeId++, "Status", AttributeKind.DERIVED)
        val assignment = AssignValue(derived, Literal("diabetic"))
        val conditions = setOf(containsText(external, "12.0"))
        val rule = Rule(0, null, conditions, mutableSetOf(), assignment)
        interp.add(rule)
        val view = ViewableInterpretation(interp)

        // When asking for the conditions of that assignment
        val result = view.conditionsForAssignment(assignment)

        // Then the condition texts are returned
        result shouldBe listOf("Glucose contains \"12.0\"")
    }

    @Test
    fun `conditionsForAssignment lists the conditions of parent rules first`() {
        // Given a rule under a parent rule, each with conditions
        val conditions0 = setOf(
            containsText(1, Attribute(26, "z"), "text z"),
            containsText(2, Attribute(1, "A"), "text A"),
            containsText(3, Attribute(25, "Y"), "text Y"),
            containsText(4, Attribute(2, "b"), "text b"),
        )
        val conditions1 = setOf(
            containsText(5, Attribute(18, "r"), "text r"),
            containsText(6, Attribute(19, "s"), "text s"),
            containsText(7, Attribute(16, "p"), "text p"),
            containsText(8, Attribute(17, "q"), "text q"),
        )
        val rule0 = Rule(0, null, conditions0, mutableSetOf(), comment(c1, "First comment"))
        val assignment = comment(c2, "Second comment")
        val rule1 = Rule(1, rule0, conditions1, mutableSetOf(), assignment)
        interp.add(rule1)
        val view = ViewableInterpretation(interp)

        // Then the parent's conditions come first, each rule's own in alphabetical order
        view.conditionsForAssignment(assignment) shouldBe listOf(
            "A contains \"text A\"",
            "b contains \"text b\"",
            "Y contains \"text Y\"",
            "z contains \"text z\"",
            "p contains \"text p\"",
            "q contains \"text q\"",
            "r contains \"text r\"",
            "s contains \"text s\""
        )
    }

    @Test
    fun `conditionsForAssignment returns empty list for a non-existent assignment`() {
        // Given a viewable interpretation with no assignments
        val view = ViewableInterpretation(interp)
        val assignment = AssignValue(Attribute(0, "X", AttributeKind.DERIVED), Literal("y"))

        // When asking for conditions of a non-existent assignment
        val result = view.conditionsForAssignment(assignment)

        // Then an empty list is returned
        result shouldBe emptyList()
    }

    private fun containsText(attribute: Attribute, match: String) = containsText(conditionId++, attribute, match)
}
