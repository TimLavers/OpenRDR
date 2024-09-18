@file:OptIn(ExperimentalTestApi::class)

package io.rippledown.interpretation

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ToolTipForIconAndTextTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    val toolTipText = "Go to Bondi Beach"
    val labelText = "Bondi"
    val isSelected = false

    @Composable
    private fun BondiToolTipAndText() {
        ToolTipForIconAndLabel(
            toolTipText = toolTipText,
            labelText = labelText,
            isSelected = isSelected,
            icon = painterResource("plus-minus_24.png"),
            onClick = { }
        )
    }

    @Test
    fun `label text should always be shown`() = runTest {
        with(composeTestRule) {
            //Given
            setContent { BondiToolTipAndText() }

            //Then
            onNodeWithText(labelText).assertIsDisplayed()
        }

    }

    @Test
    fun `tool tip text should not be shown by default`() = runTest {
        with(composeTestRule) {
            //Given
            setContent { BondiToolTipAndText() }

            //Then
            onNodeWithContentDescription(TOOLTIP_TEXT_CONTENT_DESCRIPTION).assertDoesNotExist()
        }
    }

    @Test
    fun `tool tip text should show on mouse enter`() = runTest {
        with(composeTestRule) {
            //Given
            setContent { BondiToolTipAndText() }
            onNodeWithContentDescription(TOOLTIP_TEXT_CONTENT_DESCRIPTION).assertDoesNotExist()


            //When
            onNodeWithContentDescription(TOOLTIP_AREA_CONTENT_DESCRIPTION).performMouseInput {
                moveTo(Offset.Zero)
            }

            //Then
            onNodeWithContentDescription(TOOLTIP_TEXT_CONTENT_DESCRIPTION).assertExists()

        }
    }

    @Test
    fun `badge should not show if the count is zero`() = runTest {
        with(composeTestRule) {
            //Given
            setContent { BondiToolTipAndText() }

            //Then
            onNodeWithContentDescription(BADGE_CONTENT_DESCRIPTION).assertDoesNotExist()
        }
    }
}

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
        ) {
            ToolTipForIconAndLabel(
                toolTipText = "Go to Bondi Beach",
                labelText = "Bondi",
                isSelected = true,
                icon = painterResource("plus-minus_24.png"),
                onClick = { }
            )
        }
    }
}