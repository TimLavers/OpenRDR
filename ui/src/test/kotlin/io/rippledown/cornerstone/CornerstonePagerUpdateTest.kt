package io.rippledown.cornerstone

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.coEvery
import io.mockk.mockk
import io.rippledown.model.createCase
import io.rippledown.model.rule.CornerstoneStatus
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CornerstonePagerUpdateTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: CornerstonePagerHandler

    @Before
    fun setUp() {
        handler = mockk(relaxed = true)
        coEvery { handler.selectCornerstone(any()) } returns createCase("Greta")
    }

    @Test
    fun `should show index of current cornerstone when the cornerstone status changes`() {
        with(composeTestRule) {
            //Given
            setContent {
                CornerstonePagerWithButton(handler)
            }
            requireIndexAndTotalToBeDisplayed(0, 1)

            //When
            onNodeWithText("Update Cornerstone Status").performClick()
            waitForIdle()

            //Then
            requireIndexAndTotalToBeDisplayed(1, 2)
        }
    }
}

@Composable
fun CornerstonePagerWithButton(handler: CornerstonePagerHandler) {
    val case = createCase("Greta")
    var cornerstoneStatus by remember { mutableStateOf(CornerstoneStatus(case, 0, 1)) }

    Row {
        CornerstonePager(cornerstoneStatus, handler)
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                // Update the cornerstone status
                cornerstoneStatus = CornerstoneStatus(case, 1, 2)
            }
        ) {
            Text("Update Cornerstone Status")
        }
    }
}

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
        ) {
            val handler = mockk<CornerstonePagerHandler>(relaxed = true)
            coEvery { handler.selectCornerstone(any()) } returns createCase("Greta")

            CornerstonePagerWithButton(handler)
        }
    }
}