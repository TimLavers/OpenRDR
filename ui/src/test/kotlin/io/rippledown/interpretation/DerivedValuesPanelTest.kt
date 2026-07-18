package io.rippledown.interpretation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.rippledown.constants.interpretation.DERIVED_VALUES_PANEL
import io.rippledown.constants.interpretation.DERIVED_VALUES_TOGGLE
import io.rippledown.constants.interpretation.DERIVED_VALUE_NAME_PREFIX
import io.rippledown.model.caseview.DerivedValueInfo
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

class DerivedValuesPanelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should not render anything when derived values list is empty`() = runTest {
        // Given an empty derived values list
        with(composeTestRule) {
            setContent {
                DerivedValuesPanel(derivedValues = emptyList())
            }

            // Then the toggle should not exist
            onNodeWithContentDescription(DERIVED_VALUES_TOGGLE).assertDoesNotExist()
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
