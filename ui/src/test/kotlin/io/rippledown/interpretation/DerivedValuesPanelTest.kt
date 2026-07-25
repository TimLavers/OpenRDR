package io.rippledown.interpretation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.rippledown.constants.interpretation.*
import io.rippledown.model.caseview.DerivedValueInfo
import io.rippledown.model.diff.DerivedValueAddition
import io.rippledown.model.diff.DerivedValueRemoval
import io.rippledown.model.diff.DerivedValueReplacement
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)

class DerivedValuesPanelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should show the heading and an empty state when there are no derived values`() = runTest {
        // Given an empty derived values list
        with(composeTestRule) {
            setContent {
                DerivedValuesPanel(derivedValues = emptyList())
            }

            // Then the heading and the empty-state text are shown
            onNodeWithContentDescription(DERIVED_VALUES_TOGGLE).assertIsDisplayed()
            onNodeWithContentDescription(DERIVED_ATTRIBUTES_NONE).assertIsDisplayed()
        }
    }

    @Test
    fun `should show the info icon when no derived attributes`() = runTest {
        with(composeTestRule) {
            setContent {
                DerivedValuesPanel(derivedValues = emptyList())
            }

            // Then the info icon is shown without needing to hover the heading
            onNodeWithContentDescription(DERIVED_ATTRIBUTES_INFO_ICON).assertIsDisplayed()
        }
    }

    @Test
    fun `should show the info icon when the panel has derived values`() = runTest {
        val values = listOf(
            DerivedValueInfo(name = "BMI", value = "25.3", formula = "Weight / Height", conditions = emptyList())
        )
        with(composeTestRule) {
            setContent {
                DerivedValuesPanel(derivedValues = values)
            }

            onNodeWithContentDescription(DERIVED_ATTRIBUTES_INFO_ICON).assertIsDisplayed()
        }
    }

    @Test
    fun `should not show the empty state when there are derived values`() = runTest {
        // Given a list with a derived value
        val values = listOf(
            DerivedValueInfo(name = "BMI", value = "25.3", formula = "Weight / Height", conditions = emptyList())
        )
        with(composeTestRule) {
            setContent {
                DerivedValuesPanel(derivedValues = values)
            }

            // Then the empty-state text is not shown
            onNodeWithContentDescription(DERIVED_ATTRIBUTES_NONE).assertDoesNotExist()
        }
    }

    @Test
    fun `should show panel expanded by default with derived value names and values`() = runTest {
        // Given a list of derived values
        val values = listOf(
            DerivedValueInfo(
                name = "BMI",
                value = "25.3",
                formula = "Weight / (Height * Height)",
                conditions = listOf("Weight is high")
            ),
            DerivedValueInfo(name = "Risk score", value = "high", formula = "\"high\"", conditions = emptyList())
        )
        with(composeTestRule) {
            setContent {
                DerivedValuesPanel(derivedValues = values)
            }

            // Then the panel and its content are displayed
            onNodeWithContentDescription(DERIVED_VALUES_TOGGLE).assertIsDisplayed()
            onNodeWithContentDescription(DERIVED_VALUES_PANEL).assertIsDisplayed()
            onNodeWithContentDescription("$DERIVED_VALUE_NAME_PREFIX${values[0].name}").assertIsDisplayed()
            onNodeWithText("25.3").assertIsDisplayed()
            onNodeWithContentDescription("$DERIVED_VALUE_NAME_PREFIX${values[1].name}").assertIsDisplayed()
            onNodeWithText("high").assertIsDisplayed()
        }
    }

    @Test
    fun `should collapse panel when toggle is clicked`() = runTest {
        // Given a panel with derived values
        val values = listOf(
            DerivedValueInfo(name = "BMI", value = "25.3", formula = "Weight / Height", conditions = emptyList())
        )
        with(composeTestRule) {
            setContent {
                DerivedValuesPanel(derivedValues = values)
            }

            // When the toggle is clicked
            onNodeWithContentDescription(DERIVED_VALUES_TOGGLE).performClick()
            waitForIdle()

            // Then the panel content is hidden
            onNodeWithContentDescription(DERIVED_VALUES_PANEL).assertDoesNotExist()
        }
    }

    @Test
    fun `should expand panel when toggle is clicked again`() = runTest {
        // Given a collapsed panel
        val values = listOf(
            DerivedValueInfo(name = "BMI", value = "25.3", formula = "Weight / Height", conditions = emptyList())
        )
        with(composeTestRule) {
            setContent {
                DerivedValuesPanel(derivedValues = values)
            }
            onNodeWithContentDescription(DERIVED_VALUES_TOGGLE).performClick()
            waitForIdle()

            // When the toggle is clicked again
            onNodeWithContentDescription(DERIVED_VALUES_TOGGLE).performClick()
            waitForIdle()

            // Then the panel content is shown
            onNodeWithContentDescription(DERIVED_VALUES_PANEL).assertIsDisplayed()
        }
    }

    @Test
    fun `should display multiple derived values as name-value pairs`() = runTest {
        // Given multiple derived values
        val values = listOf(
            DerivedValueInfo(name = "Alpha", value = "1", formula = "\"1\"", conditions = listOf("c1")),
            DerivedValueInfo(name = "Beta", value = "2", formula = "\"2\"", conditions = listOf("c2")),
            DerivedValueInfo(name = "Gamma", value = "3", formula = "\"3\"", conditions = listOf("c3"))
        )
        with(composeTestRule) {
            setContent {
                DerivedValuesPanel(derivedValues = values)
            }

            // Then all three name-value pairs are displayed
            values.forEach { info ->
                onNodeWithContentDescription("$DERIVED_VALUE_NAME_PREFIX${info.name}").assertIsDisplayed()
                onNodeWithText(info.value).assertIsDisplayed()
            }
        }
    }

    @Test
    fun `should show no pending state when there is no change`() = runTest {
        // Given derived values and no rule session in progress
        val values = listOf(
            DerivedValueInfo(name = "BMI", value = "30.93", formula = "weight / height ^ 2", conditions = emptyList())
        )
        with(composeTestRule) {
            setContent {
                DerivedValuesPanel(derivedValues = values)
            }

            // Then the value cell carries the plain description
            onNodeWithContentDescription("${DERIVED_VALUE_VALUE_PREFIX}BMI").assertIsDisplayed()
            onNodeWithContentDescription("${DERIVED_VALUE_PENDING_ADD_PREFIX}BMI").assertDoesNotExist()
        }
    }

    @Test
    fun `should preview a derived attribute being added`() = runTest {
        // Given a rule session that will assign a value to an attribute with none
        with(composeTestRule) {
            setContent {
                DerivedValuesPanel(
                    derivedValues = emptyList(),
                    change = DerivedValueAddition("BMI", "30.93", "weight / height ^ 2")
                )
            }

            // Then a row for it appears, marked as being added
            onNodeWithContentDescription("${DERIVED_VALUE_ROW_PREFIX}BMI").assertIsDisplayed()
            onNodeWithContentDescription("${DERIVED_VALUE_PENDING_ADD_PREFIX}BMI")
                .assertIsDisplayed()
                .assertTextEquals("30.93")
            // And the empty state is gone, since there is now something to show
            onNodeWithContentDescription(DERIVED_ATTRIBUTES_NONE).assertDoesNotExist()
        }
    }

    @Test
    fun `should preview an addition whose expression cannot be evaluated`() = runTest {
        // Given a rule session whose formula references an attribute with no value
        with(composeTestRule) {
            setContent {
                DerivedValuesPanel(
                    derivedValues = emptyList(),
                    change = DerivedValueAddition("BMI", "", "weight / height ^ 2")
                )
            }

            // Then the row still appears, with a blank value
            onNodeWithContentDescription("${DERIVED_VALUE_PENDING_ADD_PREFIX}BMI")
                .assertIsDisplayed()
                .assertTextEquals("")
        }
    }

    @Test
    fun `should insert the row being added in name order`() = runTest {
        // Given existing derived values either side of the one being added
        val values = listOf(
            DerivedValueInfo(name = "Alpha", value = "1", formula = "\"1\"", conditions = emptyList()),
            DerivedValueInfo(name = "Zeta", value = "3", formula = "\"3\"", conditions = emptyList())
        )
        with(composeTestRule) {
            setContent {
                DerivedValuesPanel(
                    derivedValues = values,
                    change = DerivedValueAddition("Mu", "2", "\"2\"")
                )
            }

            // Then all three rows are shown
            listOf("Alpha", "Mu", "Zeta").forEach {
                onNodeWithContentDescription("$DERIVED_VALUE_ROW_PREFIX$it").assertIsDisplayed()
            }
            onNodeWithContentDescription("${DERIVED_VALUE_PENDING_ADD_PREFIX}Mu").assertIsDisplayed()
        }
    }

    @Test
    fun `should preview a derived attribute being removed`() = runTest {
        // Given a rule session that will retract an existing value
        val values = listOf(
            DerivedValueInfo(name = "BMI", value = "30.93", formula = "weight / height ^ 2", conditions = emptyList()),
            DerivedValueInfo(name = "Risk", value = "high", formula = "\"high\"", conditions = emptyList())
        )
        with(composeTestRule) {
            setContent {
                DerivedValuesPanel(derivedValues = values, change = DerivedValueRemoval("BMI"))
            }

            // Then only that row is marked as being removed
            onNodeWithContentDescription("${DERIVED_VALUE_PENDING_REMOVE_PREFIX}BMI")
                .assertIsDisplayed()
                .assertTextEquals("30.93")
            onNodeWithContentDescription("${DERIVED_VALUE_VALUE_PREFIX}Risk").assertIsDisplayed()
        }
    }

    @Test
    fun `should not highlight anything when removing a value the case does not have`() = runTest {
        // Given a removal naming an attribute with no value on this case
        val values = listOf(
            DerivedValueInfo(name = "Risk", value = "high", formula = "\"high\"", conditions = emptyList())
        )
        with(composeTestRule) {
            setContent {
                DerivedValuesPanel(derivedValues = values, change = DerivedValueRemoval("BMI"))
            }

            // Then nothing is highlighted and no extra row appears
            onNodeWithContentDescription("${DERIVED_VALUE_VALUE_PREFIX}Risk").assertIsDisplayed()
            onNodeWithContentDescription("${DERIVED_VALUE_ROW_PREFIX}BMI").assertDoesNotExist()
        }
    }

    @Test
    fun `should preview a derived attribute being replaced in a single row`() = runTest {
        // Given a rule session that will change an existing value
        val values = listOf(
            DerivedValueInfo(name = "BMI", value = "30.93", formula = "weight / height ^ 2", conditions = emptyList())
        )
        with(composeTestRule) {
            setContent {
                DerivedValuesPanel(
                    derivedValues = values,
                    change = DerivedValueReplacement("BMI", "15.47", "weight / height ^ 3")
                )
            }

            // Then one row shows the old value followed by the new one
            onNodeWithContentDescription("${DERIVED_VALUE_PENDING_REPLACE_PREFIX}BMI")
                .assertIsDisplayed()
                .assertTextEquals("30.93 15.47")
        }
    }

    @Test
    fun `should still collapse and expand while a change is pending`() = runTest {
        // Given a pending addition
        with(composeTestRule) {
            setContent {
                DerivedValuesPanel(
                    derivedValues = emptyList(),
                    change = DerivedValueAddition("BMI", "30.93", "weight / height ^ 2")
                )
            }

            // When the panel is collapsed
            onNodeWithContentDescription(DERIVED_VALUES_TOGGLE).performClick()
            waitForIdle()

            // Then the pending row is hidden with the rest of the content
            onNodeWithContentDescription("${DERIVED_VALUE_PENDING_ADD_PREFIX}BMI").assertDoesNotExist()

            // And it comes back when expanded again
            onNodeWithContentDescription(DERIVED_VALUES_TOGGLE).performClick()
            waitForIdle()
            onNodeWithContentDescription("${DERIVED_VALUE_PENDING_ADD_PREFIX}BMI").assertIsDisplayed()
        }
    }
}
