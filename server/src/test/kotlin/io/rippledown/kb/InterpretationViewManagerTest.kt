package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.*
import io.rippledown.model.rule.*
import io.rippledown.utils.defaultDate
import kotlin.test.BeforeTest
import kotlin.test.Test

class InterpretationViewManagerTest {
    private lateinit var manager: InterpretationViewManager

    @BeforeTest
    fun init() {
        manager = InterpretationViewManager()
    }

    @Test
    fun `should handle comment with variable when attribute lookup fails gracefully`() {
        //Given a comment assignment whose variable attribute is not in the case
        val template = CommentTemplate("Glucose is \${}", listOf(Attribute(999, "Unknown")))
        val interpretation = interpretation(
            RuleSummary(id = 1, assignment = AssignValue(c1, template))
        )

        //When
        val viewableInterpretation = manager.viewableInterpretation(interpretation, case())

        //Then - should not crash, should render with marker for unresolved variable
        viewableInterpretation.textGivenByRules shouldNotBe null
    }

    private val glucose = Attribute(1, "Glucose")
    private val c1 = Attribute(10, "C1", AttributeKind.COMMENT)
    private val c2 = Attribute(11, "C2", AttributeKind.COMMENT)
    private val bmi = Attribute(12, "BMI", AttributeKind.DERIVED)

    private fun interpretation(vararg summaries: RuleSummary) =
        Interpretation(CaseId(42, "Hitch")).apply { summaries.forEach { add(it) } }

    private fun case(): RDRCase = with(RDRCaseBuilder()) {
        addValue(glucose, defaultDate, "12.0")
        build("Hitch")
    }

    @Test
    fun `comment attribute assignments are rendered as comments`() {
        //Given an interpretation with a comment-attribute assignment
        val interpretation = interpretation(
            RuleSummary(id = 1, assignment = AssignValue(c1, CommentTemplate("Diabetic diet advice given.")))
        )

        //When
        val viewable = manager.viewableInterpretation(interpretation, case())

        //Then
        viewable.textGivenByRules shouldBe "Diabetic diet advice given."
        viewable.renderedComments shouldBe listOf(RenderedComment("Diabetic diet advice given."))
    }

    @Test
    fun `comment template variables are rendered against the case`() {
        //Given a comment assignment with a variable
        val template = CommentTemplate("Glucose is \${} today.", listOf(glucose))
        val interpretation = interpretation(RuleSummary(id = 1, assignment = AssignValue(c1, template)))

        //When
        val viewable = manager.viewableInterpretation(interpretation, case())

        //Then the rendered comment has the case value, and the raw text keeps the token
        viewable.renderedComments shouldBe listOf(RenderedComment("Glucose is 12.0 today."))
        viewable.textGivenByRules shouldBe "Glucose is \${} today."
    }

    @Test
    fun `comment assignments are ordered by attribute id`() {
        //Given comment assignments added in reverse id order
        val interpretation = interpretation(
            RuleSummary(id = 1, assignment = AssignValue(c2, CommentTemplate("Second."))),
            RuleSummary(id = 2, assignment = AssignValue(c1, CommentTemplate("First.")))
        )

        //When
        val viewable = manager.viewableInterpretation(interpretation, case())

        //Then they are rendered in attribute id order
        viewable.textGivenByRules shouldBe "First. Second."
    }

    @Test
    fun `derived value assignments are not comments`() {
        //Given an interpretation with a derived-value assignment only
        val interpretation = interpretation(
            RuleSummary(id = 1, assignment = AssignValue(bmi, Literal("30.2")))
        )

        //When
        val viewable = manager.viewableInterpretation(interpretation, case())

        //Then no comments are shown
        viewable.textGivenByRules shouldBe ""
        viewable.renderedComments shouldBe emptyList()
    }

    @Test
    fun `a literal comment assignment is rendered as its value`() {
        //Given a comment assignment with a literal value
        val interpretation = interpretation(
            RuleSummary(id = 1, assignment = AssignValue(c1, Literal("Plain comment.")))
        )

        //When
        val viewable = manager.viewableInterpretation(interpretation, case())

        //Then
        viewable.textGivenByRules shouldBe "Plain comment."
        viewable.renderedComments shouldBe listOf(RenderedComment("Plain comment."))
    }

    @Test
    fun `a rendered comment from an assignment carries the conditions of the rule that gave it`() {
        //Given a comment assignment given by a rule with conditions
        val assignment = AssignValue(c1, CommentTemplate("Diabetic diet advice given."))
        val conditions = listOf("Glucose is high", "Age > 40")
        val interpretation = interpretation(
            RuleSummary(id = 1, assignment = assignment, conditionTextsFromRoot = conditions)
        )

        //When
        val viewable = manager.viewableInterpretation(interpretation, case())

        //Then the rendered comment carries the conditions for its tooltip
        viewable.renderedComments shouldBe listOf(
            RenderedComment("Diabetic diet advice given.", conditions = conditions)
        )
    }

    @Test
    fun `an unresolved by-definition comment assignment is skipped`() {
        //Given a comment assignment whose definition could not be resolved
        val interpretation = interpretation(
            RuleSummary(id = 1, assignment = AssignValue(c1, ByDefinition))
        )

        //When
        val viewable = manager.viewableInterpretation(interpretation, case())

        //Then no comment is shown
        viewable.textGivenByRules shouldBe ""
        viewable.renderedComments shouldBe emptyList()
    }
}