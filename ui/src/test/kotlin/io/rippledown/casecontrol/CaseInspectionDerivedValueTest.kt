package io.rippledown.casecontrol

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import io.mockk.mockk
import io.rippledown.constants.interpretation.*
import io.rippledown.interpretation.requireInterpretation
import io.rippledown.model.*
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.diff.*
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.Literal
import io.rippledown.model.rule.RuleSummary
import io.rippledown.utils.defaultDate
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

/**
 * A rule session in progress previews its pending change. A pending comment
 * change belongs in the Comments panel and a pending derived attribute change
 * in the Derived attributes panel; neither must ever appear in the other.
 */
class CaseInspectionDerivedValueTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var handler: CaseInspectionHandler

    private val glucose = Attribute(10, "Glucose", AttributeKind.EXTERNAL)
    private val bmi = Attribute(20, "BMI", AttributeKind.DERIVED)
    private val comment = Attribute(30, "Comment 1", AttributeKind.COMMENT)

    @Before
    fun setUp() {
        handler = mockk<CaseInspectionHandler>()
    }

    /**
     * A case with an existing BMI derived value, and optionally a comment.
     */
    private fun caseWithBmi(commentText: String? = null): ViewableCase {
        val assignment = AssignValue(bmi, Literal("30.93"))
        val builder = RDRCaseBuilder()
        builder.addValue(glucose, defaultDate, "12.0")
        builder.addValue(bmi, defaultDate, "30.93")
        if (commentText != null) builder.addValue(comment, defaultDate, commentText)
        val rdrCase = builder.build("Case1", 1L)

        val interpretation = Interpretation(rdrCase.caseId).apply {
            add(RuleSummary(id = 1, assignment = assignment, conditionTextsFromRoot = listOf("Glucose is high")))
            if (commentText != null) {
                add(
                    RuleSummary(
                        id = 2,
                        assignment = AssignValue(comment, Literal(commentText)),
                        conditionTextsFromRoot = listOf("Glucose is high")
                    )
                )
            }
        }
        val attributes = listOfNotNull(glucose, bmi, if (commentText != null) comment else null)
        return ViewableCase(
            rdrCase,
            CaseViewProperties(attributes),
            ViewableInterpretation(
                interpretation = interpretation,
                textGivenByRules = commentText ?: "",
                renderedComments = if (commentText == null) emptyList()
                else listOf(RenderedComment(text = commentText, unresolvedRanges = emptyList()))
            )
        )
    }

    /**
     * A case with no derived values and no comments.
     */
    private fun caseWithNothing(): ViewableCase {
        val builder = RDRCaseBuilder()
        builder.addValue(glucose, defaultDate, "12.0")
        val rdrCase = builder.build("Case1", 1L)
        return ViewableCase(rdrCase, CaseViewProperties(listOf(glucose)))
    }

    @Test
    fun `should preview a derived attribute being added in the Derived attributes panel`() = runTest {
        // Given a rule session that will assign a value to an attribute with none
        with(composeTestRule) {
            setContent {
                CaseInspection(
                    case = caseWithNothing(),
                    handler = handler,
                    derivedValueChange = DerivedValueAddition("BMI", "30.93", "weight / height ^ 2")
                )
            }

            // Then the Derived attributes panel shows it as being added
            onNodeWithContentDescription("${DERIVED_VALUE_PENDING_ADD_PREFIX}BMI", useUnmergedTree = true)
                .assertIsDisplayed()
                .assertTextEquals("30.93")
        }
    }

    @Test
    fun `should not show a pending derived attribute in the Comments panel`() = runTest {
        // Given a rule session assigning a derived value, and no comment change
        with(composeTestRule) {
            setContent {
                CaseInspection(
                    case = caseWithNothing(),
                    diff = null,
                    handler = handler,
                    derivedValueChange = DerivedValueAddition("BMI", "30.93", "weight / height ^ 2")
                )
            }

            // Then the Comments panel previews nothing. This is the regression
            // test for the attribute name leaking into the comments as a green
            // pending sentence.
            requireInterpretation("")
        }
    }

    @Test
    fun `should preview a derived attribute being removed`() = runTest {
        // Given a rule session that will retract an existing value
        with(composeTestRule) {
            setContent {
                CaseInspection(
                    case = caseWithBmi(),
                    handler = handler,
                    derivedValueChange = DerivedValueRemoval("BMI")
                )
            }

            // Then the existing row is marked as being removed
            onNodeWithContentDescription("${DERIVED_VALUE_PENDING_REMOVE_PREFIX}BMI", useUnmergedTree = true)
                .assertIsDisplayed()
                .assertTextEquals("30.93")
        }
    }

    @Test
    fun `should preview a derived attribute being replaced`() = runTest {
        // Given a rule session that will change an existing value
        with(composeTestRule) {
            setContent {
                CaseInspection(
                    case = caseWithBmi(),
                    handler = handler,
                    derivedValueChange = DerivedValueReplacement("BMI", "15.47", "weight / height ^ 3")
                )
            }

            // Then one row shows the value being replaced followed by its replacement
            onNodeWithContentDescription("${DERIVED_VALUE_PENDING_REPLACE_PREFIX}BMI", useUnmergedTree = true)
                .assertIsDisplayed()
                .assertTextEquals("30.93 15.47")
        }
    }

    @Test
    fun `should not highlight any derived value when only a comment is being changed`() = runTest {
        // Given a rule session adding a comment, with a derived value on the case
        with(composeTestRule) {
            setContent {
                CaseInspection(
                    case = caseWithBmi(),
                    diff = Addition("Go to Bondi."),
                    handler = handler,
                    derivedValueChange = null
                )
            }

            // Then the derived row is shown in its plain state
            onNodeWithContentDescription("${DERIVED_VALUE_VALUE_PREFIX}BMI", useUnmergedTree = true)
                .assertIsDisplayed()
            onNodeWithContentDescription("${DERIVED_VALUE_PENDING_ADD_PREFIX}BMI").assertDoesNotExist()
            onNodeWithContentDescription("${DERIVED_VALUE_PENDING_REMOVE_PREFIX}BMI").assertDoesNotExist()
            onNodeWithContentDescription("${DERIVED_VALUE_PENDING_REPLACE_PREFIX}BMI").assertDoesNotExist()
        }
    }

    @Test
    fun `should still preview a pending comment in the Comments panel`() = runTest {
        // Given a rule session adding a comment
        with(composeTestRule) {
            setContent {
                CaseInspection(
                    case = caseWithNothing(),
                    diff = Addition("Go to Bondi."),
                    handler = handler
                )
            }

            // Then the Comments panel previews it, as it did before derived
            // attribute previews existed
            requireInterpretation("Go to Bondi.")
        }
    }

    @Test
    fun `should route a comment diff and a derived value change to their own panels`() = runTest {
        // Given a comment change and a derived value change at the same time
        with(composeTestRule) {
            setContent {
                CaseInspection(
                    case = caseWithBmi(commentText = "Go to Bondi."),
                    diff = Removal("Go to Bondi."),
                    handler = handler,
                    derivedValueChange = DerivedValueRemoval("BMI")
                )
            }

            // Then each panel previews only its own change
            requireInterpretation("Go to Bondi.")
            onNodeWithContentDescription("${DERIVED_VALUE_PENDING_REMOVE_PREFIX}BMI", useUnmergedTree = true)
                .assertIsDisplayed()
        }
    }

    @Test
    fun `should show no preview at all when there is no rule session`() = runTest {
        // Given a case with a derived value and no session in progress
        with(composeTestRule) {
            setContent {
                CaseInspection(case = caseWithBmi(commentText = "Go to Bondi."), handler = handler)
            }

            // Then nothing is previewed in either panel
            requireInterpretation("Go to Bondi.")
            onNodeWithContentDescription("${DERIVED_VALUE_VALUE_PREFIX}BMI", useUnmergedTree = true)
                .assertIsDisplayed()
                .assertTextEquals("30.93")
        }
    }

    @Test
    fun `should show the rule conditions in the tooltip of a row being added`() = runTest {
        // Given a rule session with conditions so far
        with(composeTestRule) {
            setContent {
                CaseInspection(
                    case = caseWithNothing(),
                    ruleConditions = listOf("Glucose is high"),
                    handler = handler,
                    derivedValueChange = DerivedValueAddition("BMI", "30.93", "weight / height ^ 2")
                )
            }

            // Then the row exists, carrying those conditions for its tooltip
            onNodeWithContentDescription("${DERIVED_VALUE_ROW_PREFIX}BMI", useUnmergedTree = true)
                .assertIsDisplayed()
        }
    }
}
