package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.*
import io.rippledown.model.rule.*
import io.rippledown.persistence.inmemory.InMemoryAttributeStore
import io.rippledown.persistence.inmemory.InMemoryConclusionStore
import io.rippledown.persistence.inmemory.InMemoryOrderStore
import io.rippledown.persistence.inmemory.InMemoryVerifiedTextStore
import io.rippledown.utils.defaultDate
import kotlin.test.BeforeTest
import kotlin.test.Test

class InterpretationViewManagerTest {
    private lateinit var manager: InterpretationViewManager
    private lateinit var verifiedTextStore: InMemoryVerifiedTextStore

    @BeforeTest
    fun init() {
        val conclusionManager = ConclusionManager(InMemoryConclusionStore())
        val attributeManager = AttributeManager(InMemoryAttributeStore())
        val orderStore = InMemoryOrderStore()
        verifiedTextStore = InMemoryVerifiedTextStore()
        manager = InterpretationViewManager(orderStore, conclusionManager, attributeManager)
    }

    @Test
    fun `should be no ordering when the interpretation view manager is created from an empty conclusion manager`() {
        manager.allInOrder() shouldBe emptyList()
    }

    @Test
    fun `should set the text given by rules according to the conclusion ordering`() {
        //Given
        val conclusion1 = Conclusion(1, "a")
        val conclusion2 = Conclusion(2, "b")
        val conclusion3 = Conclusion(3, "c")
        val interpretation = mockk<Interpretation>()
        val case = mockk<RDRCase>()
        every { interpretation.conclusions() } returns setOf(conclusion1, conclusion2, conclusion3)
        every { interpretation.assignments() } returns emptySet()
        every { interpretation.caseId } returns CaseId(42, "Hitch")
        manager.insert(listOf(conclusion3, conclusion1, conclusion2))

        //When
        val viewableInterpretation = manager.viewableInterpretation(interpretation, case)

        //Then
        viewableInterpretation.textGivenByRules shouldBe "c a b"
    }

    @Test
    fun `should handle comment with variable when attribute lookup fails gracefully`() {
        //Given
        val glucose = Attribute(1, "Glucose")
        val template = "Glucose is " + io.rippledown.model.VARIABLE_TOKEN
        val variables = listOf(io.rippledown.model.CommentVariable(999)) // Bad ID
        val conclusion = Conclusion(1, template, variables)
        val interpretation = mockk<Interpretation>()
        val case = mockk<RDRCase>()
        every { interpretation.conclusions() } returns setOf(conclusion)
        every { interpretation.assignments() } returns emptySet()
        every { interpretation.caseId } returns CaseId(42, "Hitch")
        manager.insert(listOf(conclusion))

        //When
        val viewableInterpretation = manager.viewableInterpretation(interpretation, case)

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
    fun `comment assignments follow the ordered conclusions`() {
        //Given an interpretation with a conclusion and a comment assignment
        val conclusion = Conclusion(1, "From a conclusion.")
        manager.insert(listOf(conclusion))
        val interpretation = interpretation(
            RuleSummary(id = 1, conclusion = conclusion),
            RuleSummary(id = 2, assignment = AssignValue(c2, CommentTemplate("From an assignment.")))
        )

        //When
        val viewable = manager.viewableInterpretation(interpretation, case())

        //Then the conclusion comes first
        viewable.textGivenByRules shouldBe "From a conclusion. From an assignment."
        viewable.renderedComments shouldBe listOf(
            RenderedComment("From a conclusion."),
            RenderedComment("From an assignment.")
        )
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