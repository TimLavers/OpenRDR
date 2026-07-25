package io.rippledown.interpretation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.rippledown.constants.interpretation.*
import io.rippledown.model.caseview.DerivedValueInfo
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
    fun `should not show the info icon until the heading is hovered`() = runTest {
        with(composeTestRule) {
            setContent {
                DerivedValuesPanel(derivedValues = emptyList())
            }

            // Then the info icon is not shown initially
            onNodeWithContentDescription(DERIVED_ATTRIBUTES_INFO_ICON).assertDoesNotExist()

            // When the heading is hovered
            onNodeWithContentDescription(DERIVED_VALUES_TOGGLE).performMouseInput { enter(center) }
            waitForIdle()

            // Then the info icon appears
            onNodeWithContentDescription(DERIVED_ATTRIBUTES_INFO_ICON).assertIsDisplayed()
        }
    }

    @Test
    fun `clicking the info icon should toggle the derived attributes explainer`() = runTest {
        with(composeTestRule) {
            setContent {
                DerivedValuesPanel(derivedValues = emptyList())
            }
            onNodeWithContentDescription(DERIVED_VALUES_TOGGLE).performMouseInput { enter(center) }
            waitForIdle()

            // When the info icon is clicked
            onNodeWithContentDescription(DERIVED_ATTRIBUTES_INFO_ICON).performClick()
            waitForIdle()

            // Then the explainer is shown
            onNodeWithContentDescription(DERIVED_ATTRIBUTES_HELP).assertIsDisplayed()

            // When the info icon is clicked again
            onNodeWithContentDescription(DERIVED_ATTRIBUTES_INFO_ICON).performClick()
            waitForIdle()

            // Then the explainer is hidden
            onNodeWithContentDescription(DERIVED_ATTRIBUTES_HELP).assertDoesNotExist()
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
}
