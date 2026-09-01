package io.rippledown.model.caseview

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.Literal
import io.rippledown.model.rule.RuleSummary
import io.rippledown.utils.*
import kotlinx.serialization.json.Json
import kotlin.test.Test

class ViewableCaseTest {
    val abc = Attribute(1, "ABC")
    val tsh = Attribute(2, "TSH")
    private val xyz = Attribute(3, "XYZ")

    @Test
    fun construction() {
        val rdrCase = createCase("Case1")
        val viewableCase = ViewableCase(rdrCase, caseViewProperties())
        viewableCase.case shouldBe rdrCase
        viewableCase.viewProperties shouldBe caseViewProperties()
    }

    @Test
    fun `construction refuses a viewable interpretation with different rules`() {
        // Given a case and a viewable interpretation for the same case id but different rule results
        val rdrCase = createCase("Case1")
        val differentInterpretation = Interpretation(rdrCase.caseId).apply {
            add(RuleSummary(id = 99, assignment = AssignValue(tsh, Literal("different"))))
        }

        // When a viewable case is constructed from the mismatched interpretation
        // Then the mismatch is refused even though the case ids agree
        shouldThrow<IllegalStateException> {
            ViewableCase(
                rdrCase,
                caseViewProperties(),
                ViewableInterpretation(differentInterpretation)
            )
        }
    }

    @Test
    fun name() {
        ViewableCase(createCase("Case1"), caseViewProperties()).name shouldBe "Case1"
    }

    @Test
    fun attributes() {
        val properties = caseViewProperties()
        val viewableCase = ViewableCase(createCase("Case1"), properties)
        viewableCase.attributes() shouldBe listOf(abc, tsh, xyz)
    }

    @Test
    fun dates() {
        val builder = RDRCaseBuilder()
        val tshResult1 = Result(Value("0.67"), null, "mU/L")
        builder.addResult(tsh, defaultDate, tshResult1)
        val tshResult0 = Result(Value("0.08"), null, "mU/L")
        val yesterday = daysAgo(1)
        builder.addResult(tsh, yesterday, tshResult0)
        val properties = CaseViewProperties(listOf(tsh))
        val viewableCase = ViewableCase(builder.build("Case1"), properties)
        viewableCase.dates shouldBe listOf(yesterday, defaultDate)
    }

   @Test
    fun numberOfColumns() {
        val builder = RDRCaseBuilder()
        builder.addValue(tsh, defaultDate, "1.0")
        builder.addValue(tsh, daysAgo(1), "2.0")
        val properties = CaseViewProperties(listOf(tsh))
        val viewableCase = ViewableCase(builder.build("Case1"), properties)
        viewableCase.numberOfColumns shouldBe 2
    }

    @Test
    fun serialization() {
        val properties = caseViewProperties()
        val viewableCase = ViewableCase(createCase("Case1"), properties)
        val format = Json { allowStructuredMapKeys = true }
        val serialized = format.encodeToString(viewableCase)
        val deserialized = format.decodeFromString<ViewableCase>(serialized)
        deserialized shouldBe viewableCase
    }

    @Test
    fun serializationWithInterpretation() {
        val surfComment = "Surf's up."
        val viewableCase = createViewableCaseWithInterpretation("Case1", 123, listOf(surfComment))
        withClue("sanity check") {
            viewableCase.latestText() shouldBe surfComment
        }

        val deserialized = serializeDeserialize(viewableCase)
        deserialized shouldBe viewableCase
        deserialized.latestText() shouldBe surfComment

        checkSerializationIsThreadSafe(viewableCase)
    }

    @Test
    fun `derivedValues returns empty list when there are no assignments`() {
        // Given a case with no derived attribute assignments
        val viewableCase = ViewableCase(createCase("Case1"), caseViewProperties())

        // When asking for derived values
        val result = viewableCase.derivedValues()

        // Then an empty list is returned
        result shouldBe emptyList()
    }

    @Test
    fun `derivedValues returns non-comment derived attributes with value formula and conditions`() {
        // Given a case with a derived attribute assignment
        val glucose = Attribute(10, "Glucose", AttributeKind.EXTERNAL)
        val diabetesStatus = Attribute(20, "Diabetes status", AttributeKind.DERIVED)
        val assignment = AssignValue(diabetesStatus, Literal("diabetic"))
        val conditions = listOf("Glucose is \"12.0\"")

        val builder = RDRCaseBuilder()
        builder.addValue(glucose, defaultDate, "12.0")
        builder.addValue(diabetesStatus, defaultDate, "diabetic")
        val rdrCase = builder.build("Case1")

        val interp = Interpretation(rdrCase.caseId).apply {
            add(
                RuleSummary(
                    id = 1,
                    assignment = assignment,
                    conditionTextsFromRoot = conditions
                )
            )
        }
        val viewableInterp = ViewableInterpretation(
            interpretation = interp,
            textGivenByRules = "",
            renderedComments = emptyList()
        )
        val viewableCase = ViewableCase(
            rdrCase,
            CaseViewProperties(listOf(glucose, diabetesStatus)),
            viewableInterp
        )

        // When asking for derived values
        val result = viewableCase.derivedValues()

        // Then the derived value info is returned with name, value, formula, and conditions
        result.size shouldBe 1
        val info = result.first()
        info.name shouldBe "Diabetes status"
        info.value shouldBe "diabetic"
        info.formula shouldBe "\"diabetic\""
        info.conditions shouldBe conditions
    }

    @Test
    fun `derivedValues excludes comment-kind attributes`() {
        // Given a case with both a derived and a comment attribute assignment
        val glucose = Attribute(10, "Glucose", AttributeKind.EXTERNAL)
        val diabetesStatus = Attribute(20, "Diabetes status", AttributeKind.DERIVED)
        val comment1 = Attribute(30, "Comment 1", AttributeKind.COMMENT)
        val derivedAssignment = AssignValue(diabetesStatus, Literal("diabetic"))
        val commentAssignment = AssignValue(comment1, Literal("some comment"))

        val builder = RDRCaseBuilder()
        builder.addValue(glucose, defaultDate, "12.0")
        builder.addValue(diabetesStatus, defaultDate, "diabetic")
        builder.addValue(comment1, defaultDate, "some comment")
        val rdrCase = builder.build("Case1")

        val interp = Interpretation(rdrCase.caseId).apply {
            add(RuleSummary(id = 1, assignment = derivedAssignment, conditionTextsFromRoot = listOf("Glucose is high")))
            add(RuleSummary(id = 2, assignment = commentAssignment, conditionTextsFromRoot = listOf("Glucose is low")))
        }
        val viewableInterp = ViewableInterpretation(
            interpretation = interp,
            textGivenByRules = "some comment",
            renderedComments = listOf(RenderedComment(text = "some comment", unresolvedRanges = emptyList()))
        )
        val viewableCase = ViewableCase(
            rdrCase,
            CaseViewProperties(listOf(glucose, diabetesStatus, comment1)),
            viewableInterp
        )

        // When asking for derived values
        val result = viewableCase.derivedValues()

        // Then only the DERIVED-kind attribute is included, not the COMMENT-kind one
        result.size shouldBe 1
        result.first().name shouldBe "Diabetes status"
    }

    @Test
    fun `derivedValues sorts by attribute name`() {
        // Given a case with two derived attributes in non-alphabetical order
        val glucose = Attribute(10, "Glucose", AttributeKind.EXTERNAL)
        val zebra = Attribute(20, "Zebra score", AttributeKind.DERIVED)
        val alpha = Attribute(30, "Alpha index", AttributeKind.DERIVED)

        val builder = RDRCaseBuilder()
        builder.addValue(glucose, defaultDate, "5.0")
        builder.addValue(zebra, defaultDate, "42")
        builder.addValue(alpha, defaultDate, "1")
        val rdrCase = builder.build("Case1")

        val interp = Interpretation(rdrCase.caseId).apply {
            add(
                RuleSummary(
                    id = 1,
                    assignment = AssignValue(zebra, Literal("42")),
                    conditionTextsFromRoot = listOf("c1")
                )
            )
            add(
                RuleSummary(
                    id = 2,
                    assignment = AssignValue(alpha, Literal("1")),
                    conditionTextsFromRoot = listOf("c2")
                )
            )
        }
        val viewableInterp = ViewableInterpretation(
            interpretation = interp,
            textGivenByRules = "",
            renderedComments = emptyList()
        )
        val viewableCase = ViewableCase(
            rdrCase,
            CaseViewProperties(listOf(glucose, zebra, alpha)),
            viewableInterp
        )

        // When asking for derived values
        val result = viewableCase.derivedValues()

        // Then they are sorted by name
        result.map { it.name } shouldBe listOf("Alpha index", "Zebra score")
    }

    private fun caseViewProperties() = CaseViewProperties(listOf(abc, tsh, xyz))

    private fun createCase(name: String): RDRCase {
        val builder = RDRCaseBuilder()
        builder.addValue(tsh, defaultDate, "0.68")
        builder.addValue(xyz, defaultDate, "0.66")
        builder.addValue(abc, defaultDate, "0.67")
        return builder.build(name)
    }
}