package io.rippledown.main

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

/**
 * Regression test for the recomposition bug where `Main.kt` constructed
 * `Handler` (and therefore `Api`) inline in the `Window` content lambda.
 *
 * When a rule session started, `cornerstoneStatus` flipped, the handler's
 * `setWindowSize(true)` resized the window to `EXPANDED_WINDOW_SIZE`, the
 * `WindowState` mutated and Compose recomposed the content lambda. That
 * created a fresh `Api` whose `currentKB` was `null`, silently dropping
 * the selected KB and routing the next chat request to the server's
 * default KB ("Thyroids") whose `ChatSessionManager` had never had
 * `startConversation` called -> the chat panel showed "No conversation
 * is active for this knowledge base."
 *
 * The fix wraps the handler construction in `remember`. This test
 * verifies that wrapper, exercised through [rememberMainHandler], by
 * forcing several recompositions and asserting the same `Handler` (and
 * the same `Api`) is returned every time.
 */
@OptIn(ExperimentalTestApi::class)
class MainHandlerStabilityTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `rememberMainHandler returns the same Handler and Api across recompositions`() {
        var trigger by mutableStateOf(0)
        val handlersSeen = mutableListOf<Handler>()
        val apisSeen = mutableListOf<Api>()

        val fakeApi: Api = mockk(relaxed = true)

        composeTestRule.setContent {
            // Read `trigger` so this composable scope recomposes when it changes.
            @Suppress("UNUSED_VARIABLE") val read = trigger
            val handler = rememberMainHandler(
                isClosing = { false },
                resizeWindow = { /* no-op for the test */ },
                apiFactory = { fakeApi }
            )
            // Capture in a SideEffect so we only observe values from
            // committed compositions, not aborted ones.
            SideEffect {
                handlersSeen += handler
                apisSeen += handler.api
            }
        }

        composeTestRule.runOnIdle { trigger = 1 }
        composeTestRule.runOnIdle { trigger = 2 }
        composeTestRule.runOnIdle { trigger = 3 }

        composeTestRule.runOnIdle {
            // We must have observed at least two compositions (initial +
            // at least one triggered recomposition).
            (handlersSeen.size >= 2) shouldBe true
            // Every observation should be the exact same Handler instance.
            handlersSeen.toSet().size shouldBe 1
            // And the exact same Api instance.
            apisSeen.toSet().size shouldBe 1
            apisSeen.first() shouldBe fakeApi
        }
    }

    @Test
    fun `setWindowSize delegates to the supplied resizeWindow callback`() {
        val sizes = mutableListOf<androidx.compose.ui.unit.DpSize>()
        var handlerOut: Handler? = null

        composeTestRule.setContent {
            handlerOut = rememberMainHandler(
                isClosing = { false },
                resizeWindow = { sizes += it },
                apiFactory = { mockk(relaxed = true) }
            )
        }

        composeTestRule.runOnIdle {
            handlerOut!!.setWindowSize(isShowingCornerstone = true)
            handlerOut!!.setWindowSize(isShowingCornerstone = false)
        }

        composeTestRule.runOnIdle {
            sizes shouldBe listOf(EXPANDED_WINDOW_SIZE, DEFAULT_WINDOW_SIZE)
        }
    }
}
