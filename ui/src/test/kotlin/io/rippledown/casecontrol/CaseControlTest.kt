package io.rippledown.casecontrol

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import io.rippledown.constants.caseview.CASE_VIEW_FILTER_CLEAR_DESCRIPTION
import io.rippledown.constants.caseview.CASE_VIEW_FILTER_FIELD_DESCRIPTION
import io.rippledown.constants.caseview.CASE_VIEW_TABLE
import io.rippledown.constants.cornerstone.CORNERSTONE_TITLE
import io.rippledown.cornerstone.*
import io.rippledown.interpretation.requireInterpretation
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.utils.createViewableCase
import io.rippledown.utils.createViewableCaseWithInterpretation
import io.rippledown.utils.defaultDate
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class CaseControlTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: CaseControlHandler

    @Before
    fun setUp() {
        handler = mockk<CaseControlHandler>()
    }

    @Test
    fun `should show the interpretative report of the case`() = runTest {
        val name = "case A"
        val bondiComment = "Go to Bondi"
        val case = createViewableCaseWithInterpretation(name, 1, listOf(bondiComment))

        with(composeTestRule) {
            //Given
            setContent {
                CaseControl(
                    currentCase = case,
                    handler = handler
                )
            }

            //Then
            requireInterpretation(bondiComment)
        }
    }

    @Test
    fun `should show case view`() = runTest {
        val viewableCase = createViewableCaseWithInterpretation(
            name = "case 1",
            caseId = 1,
            conclusionTexts = listOf()
        )

        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = viewableCase,
                    handler = handler
                )
            }
            waitForCaseToBeShowing("case 1")
        }
    }

    @Test
    fun `should show cornerstone index and total when cornerstone status is provided`() = runTest {
        val currentCase = createViewableCaseWithInterpretation("case A", 1, listOf())
        val cornerstoneCase = createViewableCase("cornerstone B", 2)
        val status = CornerstoneStatus(
            cornerstoneToReview = cornerstoneCase,
            indexOfCornerstoneToReview = 0,
            numberOfCornerstones = 3
        )

        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = currentCase,
                    cornerstoneStatus = status,
                    handler = handler
                )
            }
            requireCornerstoneCase("cornerstone B")
            requireCornerstoneLabel("$CORNERSTONE_TITLE 1 of 3")
        }
    }

    @Test
    fun `should show correct index for second cornerstone of five`() = runTest {
        val currentCase = createViewableCaseWithInterpretation("case A", 1, listOf())
        val cornerstoneCase = createViewableCase("cornerstone C", 3)
        val status = CornerstoneStatus(
            cornerstoneToReview = cornerstoneCase,
            indexOfCornerstoneToReview = 1,
            numberOfCornerstones = 5
        )

        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = currentCase,
                    cornerstoneStatus = status,
                    handler = handler
                )
            }
            requireCornerstoneLabel("$CORNERSTONE_TITLE 2 of 5")
        }
    }

    @Test
    fun `should show correct index for last cornerstone`() = runTest {
        val currentCase = createViewableCaseWithInterpretation("case A", 1, listOf())
        val cornerstoneCase = createViewableCase("cornerstone D", 4)
        val status = CornerstoneStatus(
            cornerstoneToReview = cornerstoneCase,
            indexOfCornerstoneToReview = 2,
            numberOfCornerstones = 3
        )

        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = currentCase,
                    cornerstoneStatus = status,
                    handler = handler
                )
            }
            requireCornerstoneLabel("$CORNERSTONE_TITLE 3 of 3")
        }
    }

    @Test
    fun `should not show cornerstone index and total when no cornerstone status`() = runTest {
        val currentCase = createViewableCaseWithInterpretation("case A", 1, listOf())

        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = currentCase,
                    handler = handler
                )
            }
            requireNoCornerstoneLabel()
        }
    }

    @Test
    fun `should show no cornerstones to review message when cornerstone status has no cornerstone to review`() =
        runTest {
            val currentCase = createViewableCaseWithInterpretation("case A", 1, listOf())
            val status = CornerstoneStatus()

            with(composeTestRule) {
                setContent {
                    CaseControl(
                        currentCase = currentCase,
                        cornerstoneStatus = status,
                        handler = handler
                    )
                }
                requireNoCornerstonesToReviewMessage()
            }
        }

    @Test
    fun `should not show no cornerstones to review message when cornerstone status is null`() = runTest {
        val currentCase = createViewableCaseWithInterpretation("case A", 1, listOf())

        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = currentCase,
                    handler = handler
                )
            }
            requireNoNoCornerstonesToReviewMessage()
        }
    }

    @Test
    fun `should not show no cornerstones to review message when there are cornerstones`() = runTest {
        val currentCase = createViewableCaseWithInterpretation("case A", 1, listOf())
        val cornerstoneCase = createViewableCase("cornerstone B", 2)
        val status = CornerstoneStatus(
            cornerstoneToReview = cornerstoneCase,
            indexOfCornerstoneToReview = 0,
            numberOfCornerstones = 3
        )

        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = currentCase,
                    cornerstoneStatus = status,
                    handler = handler
                )
            }
            requireNoNoCornerstonesToReviewMessage()
        }
    }

    // ---------------------------------------------------------------------
    // Filter widget — the case-control owns one filter that applies to the
    // current case AND any cornerstone case shown beside it. The filter must
    // persist across case selection and across cornerstone changes, because
    // it is a view-state concern, not a per-case concern.
    // ---------------------------------------------------------------------

    private val hgb = Attribute(101, "HGB")
    private val mcv = Attribute(102, "MCV")
    private val ast = Attribute(103, "AST")

    // Scope text-match assertions to nodes inside a case-view table, so the
    // filter field's own EditableText (which mirrors the typed query) does
    // not count toward attribute-row totals.
    private fun ComposeTestRule.attributeRowsWithText(name: String): SemanticsNodeInteractionCollection =
        onAllNodes(hasText(name) and hasAnyAncestor(hasContentDescription(CASE_VIEW_TABLE)))

    private fun bloodCase(name: String, hgbValue: String, mcvValue: String, astValue: String): ViewableCase {
        val builder = RDRCaseBuilder()
        builder.addValue(hgb, defaultDate, hgbValue)
        builder.addValue(mcv, defaultDate, mcvValue)
        builder.addValue(ast, defaultDate, astValue)
        return ViewableCase(builder.build(name), CaseViewProperties(listOf(hgb, mcv, ast)))
    }

    @Test
    fun `the case-control renders a filter field above the case panels`() = runTest {
        val currentCase = bloodCase("main", "194", "100.2", "23")

        with(composeTestRule) {
            setContent { CaseControl(currentCase = currentCase, handler = handler) }
            onNodeWithContentDescription(CASE_VIEW_FILTER_FIELD_DESCRIPTION).assertExists()
        }
    }

    @Test
    fun `typing into the filter restricts the current case to matching rows`() = runTest {
        val currentCase = bloodCase("main", "194", "100.2", "23")

        with(composeTestRule) {
            setContent { CaseControl(currentCase = currentCase, handler = handler) }
            onNodeWithContentDescription(CASE_VIEW_FILTER_FIELD_DESCRIPTION).performTextInput("MCV")

            attributeRowsWithText(mcv.name).assertCountEquals(1)
            attributeRowsWithText(hgb.name).assertCountEquals(0)
            attributeRowsWithText(ast.name).assertCountEquals(0)
        }
    }

    @Test
    fun `the same filter is applied simultaneously to the cornerstone case`() = runTest {
        val currentCase = bloodCase("main", "194", "100.2", "23")
        val cornerstone = bloodCase("cornerstone", "150", "92", "20")
        val status = CornerstoneStatus(
            cornerstoneToReview = cornerstone,
            indexOfCornerstoneToReview = 0,
            numberOfCornerstones = 1
        )

        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = currentCase,
                    cornerstoneStatus = status,
                    handler = handler
                )
            }
            onNodeWithContentDescription(CASE_VIEW_FILTER_FIELD_DESCRIPTION).performTextInput("MCV")

            // MCV is shown once in each of the two panels
            attributeRowsWithText(mcv.name).assertCountEquals(2)
            attributeRowsWithText(hgb.name).assertCountEquals(0)
            attributeRowsWithText(ast.name).assertCountEquals(0)
        }
    }

    @Test
    fun `filter persists when the current case is replaced`() = runTest {
        val firstCase = bloodCase("first", "194", "100.2", "23")
        val secondCase = bloodCase("second", "150", "92", "20")
        var current by mutableStateOf(firstCase)

        with(composeTestRule) {
            setContent { CaseControl(currentCase = current, handler = handler) }
            onNodeWithContentDescription(CASE_VIEW_FILTER_FIELD_DESCRIPTION).performTextInput("MCV")
            attributeRowsWithText(mcv.name).assertCountEquals(1)

            // When the selected case is replaced
            current = secondCase

            // Then the new case is still filtered to the same query
            attributeRowsWithText(mcv.name).assertCountEquals(1)
            attributeRowsWithText(hgb.name).assertCountEquals(0)
            attributeRowsWithText(ast.name).assertCountEquals(0)
        }
    }

    @Test
    fun `filter persists when the cornerstone case changes`() = runTest {
        val currentCase = bloodCase("main", "194", "100.2", "23")
        val firstCornerstone = bloodCase("cornerstone-a", "150", "92", "20")
        val secondCornerstone = bloodCase("cornerstone-b", "130", "88", "30")
        var status by mutableStateOf(
            CornerstoneStatus(
                cornerstoneToReview = firstCornerstone,
                indexOfCornerstoneToReview = 0,
                numberOfCornerstones = 2
            )
        )

        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = currentCase,
                    cornerstoneStatus = status,
                    handler = handler
                )
            }
            onNodeWithContentDescription(CASE_VIEW_FILTER_FIELD_DESCRIPTION).performTextInput("MCV")
            attributeRowsWithText(mcv.name).assertCountEquals(2)

            // When the cornerstone under review changes
            status = CornerstoneStatus(
                cornerstoneToReview = secondCornerstone,
                indexOfCornerstoneToReview = 1,
                numberOfCornerstones = 2
            )

            // Then the filter is still active for the new cornerstone too
            attributeRowsWithText(mcv.name).assertCountEquals(2)
            attributeRowsWithText(hgb.name).assertCountEquals(0)
        }
    }

    @Test
    fun `clearing the filter restores all rows in both panels`() = runTest {
        val currentCase = bloodCase("main", "194", "100.2", "23")
        val cornerstone = bloodCase("cornerstone", "150", "92", "20")
        val status = CornerstoneStatus(
            cornerstoneToReview = cornerstone,
            indexOfCornerstoneToReview = 0,
            numberOfCornerstones = 1
        )

        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = currentCase,
                    cornerstoneStatus = status,
                    handler = handler
                )
            }
            onNodeWithContentDescription(CASE_VIEW_FILTER_FIELD_DESCRIPTION).performTextInput("MCV")
            attributeRowsWithText(hgb.name).assertCountEquals(0)

            // When the user clicks the clear button
            onNodeWithContentDescription(CASE_VIEW_FILTER_CLEAR_DESCRIPTION).performClick()

            // Then every attribute is showing again in both panels
            attributeRowsWithText(hgb.name).assertCountEquals(2)
            attributeRowsWithText(mcv.name).assertCountEquals(2)
            attributeRowsWithText(ast.name).assertCountEquals(2)
        }
    }

    @Test
    fun `the clear button is not present when the filter is empty`() = runTest {
        val currentCase = bloodCase("main", "194", "100.2", "23")

        with(composeTestRule) {
            setContent { CaseControl(currentCase = currentCase, handler = handler) }
            onNodeWithContentDescription(CASE_VIEW_FILTER_CLEAR_DESCRIPTION).assertDoesNotExist()
        }
    }

    @Test
    fun `replacing the filter text via text replacement re-filters both panels`() = runTest {
        // Defensive: belt-and-braces that simulating a new value (not just append) also works.
        val currentCase = bloodCase("main", "194", "100.2", "23")
        val cornerstone = bloodCase("cornerstone", "150", "92", "20")
        val status = CornerstoneStatus(
            cornerstoneToReview = cornerstone,
            indexOfCornerstoneToReview = 0,
            numberOfCornerstones = 1
        )

        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = currentCase,
                    cornerstoneStatus = status,
                    handler = handler
                )
            }
            onNodeWithContentDescription(CASE_VIEW_FILTER_FIELD_DESCRIPTION).performTextInput("MCV")
            attributeRowsWithText(mcv.name).assertCountEquals(2)

            onNodeWithContentDescription(CASE_VIEW_FILTER_FIELD_DESCRIPTION).performTextReplacement("AST")
            attributeRowsWithText(ast.name).assertCountEquals(2)
            attributeRowsWithText(mcv.name).assertCountEquals(0)
        }
    }
}