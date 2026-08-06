package io.rippledown.interpretation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import io.rippledown.constants.interpretation.DERIVED_VALUE_NAME_PREFIX
import io.rippledown.constants.interpretation.DERIVED_VALUE_ROW_PREFIX
import io.rippledown.model.caseview.DerivedValueInfo
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

class DerivedValueRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders the derived attribute name and value`() = runTest {
        val info = DerivedValueInfo(
            name = "BMI",
            value = "25.3",
            formula = "Weight / (Height * Height)",
            conditions = listOf("Weight is high")
        )

        with(composeTestRule) {
            setContent { DerivedValueRow(DerivedValueRowState(info)) }

            onNodeWithText(info.value).assertIsDisplayed()
            onNodeWithContentDescription("$DERIVED_VALUE_NAME_PREFIX${info.name}")
                .assertIsDisplayed()
                .assertTextEquals(info.name)
            onNodeWithContentDescription("$DERIVED_VALUE_ROW_PREFIX${info.name}")
                .assertIsDisplayed()
        }
    }
}
