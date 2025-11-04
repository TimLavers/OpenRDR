@file:OptIn(ExperimentalCoroutinesApi::class)

package io.rippledown.cornerstone

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.model.rule.CornerstoneStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class CornerstonePollerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: CornerstonePollerHandler

    @Before
    fun setUp() {
        handler = mockk<CornerstonePollerHandler>()
    }

    @Test
    fun `should call handler for updated CornerstoneStatus`() = runTest {
        //Given
        val cornerstoneStatus = mockk<CornerstoneStatus>()
        every { handler.updateCornerstoneStatus() } returns cornerstoneStatus
        every { handler.isClosing() } returns false

        //When
        launch(Dispatchers.Default) {
            with(composeTestRule) {
                setContent {
                    CornerstonePoller(handler)
                }
            }
        }

        //Then
        verify(timeout = 2_000) { handler.onUpdate(cornerstoneStatus) }

    }
}