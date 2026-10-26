package io.rippledown.appbar

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.rippledown.constants.main.APPLICATION_BAR_ID
import io.rippledown.constants.kb.PRESENCE_DESCRIPTION
import io.rippledown.model.KBInfo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.jetbrains.skia.Image
import java.io.File

@OptIn(ExperimentalTestApi::class)
class ApplicationBarPresenceTest {
    @Test
    fun `presence indicator shows a count and names on hover`() = runComposeUiTest {
        // Given
        val others = mutableStateOf(emptyList<String>())
        setContent { ApplicationBar(KBInfo("Clinic"), othersPresent = others.value) }
        val presence = onNodeWithContentDescription(PRESENCE_DESCRIPTION)
        presence.assertDoesNotExist()

        // When
        runOnIdle { others.value = listOf("Bob", "Carol") }

        // Then
        presence.assertIsDisplayed().assertTextEquals("2").assertHasClickAction()
        val details = "Others with Clinic open:\n• Bob\n• Carol"
        presence.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, details))
        onNodeWithText(details).assertDoesNotExist()
        presence.fetchSemanticsNode().boundsInRoot.left shouldBeGreaterThan
                (onNodeWithTag(APPLICATION_BAR_ID).fetchSemanticsNode().boundsInRoot.width / 2)

        // When the people icon is hovered
        presence.performMouseInput { moveTo(center) }

        // Then the heading ends in a colon and each name is on its own bulleted line
        waitUntil(timeoutMillis = 3_000) { onAllNodesWithText(details).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText(details).assertIsDisplayed()
        listOf("bar" to onNodeWithTag(APPLICATION_BAR_ID), "popup" to onNodeWithText(details)).forEach { (name, node) ->
            val screenshot = Image.makeFromBitmap(node.captureToImage().asSkiaBitmap())
            File("build/reports/presence-$name.png").apply { parentFile.mkdirs() }
                .writeBytes(checkNotNull(screenshot.encodeToData()).bytes)
        }

        // When
        runOnIdle { others.value = listOf("Bob") }

        // Then
        presence.assertTextEquals("1")
        onNodeWithText("Others with Clinic open:\n• Bob").assertIsDisplayed()
        onNodeWithText(details).assertDoesNotExist()

        // When
        runOnIdle { others.value = emptyList() }

        // Then
        presence.assertDoesNotExist()
        onNodeWithText("Others with Clinic open:\n• Bob").assertDoesNotExist()
    }

    @Test
    fun `click opens the names and a second click dismisses them`() = runComposeUiTest {
        // Given
        setContent { ApplicationBar(KBInfo("Clinic"), othersPresent = listOf("Bob")) }
        val presence = onNodeWithContentDescription(PRESENCE_DESCRIPTION)
        val details = "Others with Clinic open:\n• Bob"

        // When
        presence.performClick()

        // Then
        onNodeWithText(details).assertIsDisplayed()

        // When
        presence.performClick()

        // Then
        onNodeWithText(details).assertDoesNotExist()
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `click keeps names visible when hover or focus has already revealed them`(focus: Boolean) = runComposeUiTest {
        // Given the names are already visible through hover or keyboard focus
        setContent { ApplicationBar(KBInfo("Clinic"), othersPresent = listOf("Bob")) }
        val presence = onNodeWithContentDescription(PRESENCE_DESCRIPTION)
        val details = "Others with Clinic open:\n• Bob"
        if (focus) presence.performSemanticsAction(SemanticsActions.RequestFocus)
        else presence.performMouseInput { moveTo(center) }
        waitUntil(timeoutMillis = 3_000) { onAllNodesWithText(details).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText(details).assertIsDisplayed()

        // When the user clicks to open the list
        presence.performClick()

        // Then the first click keeps the names visible
        onNodeWithText(details).assertIsDisplayed()
    }

    @Test
    fun `keyboard focus reveals names and changing the KB dismisses the old popup`() = runComposeUiTest {
        // Given
        val selected = mutableStateOf<KBInfo?>(KBInfo("clinic", "Clinic"))
        val others = mutableStateOf(listOf("Bob"))
        setContent { ApplicationBar(selected.value, othersPresent = others.value) }
        val presence = onNodeWithContentDescription(PRESENCE_DESCRIPTION)

        // When
        presence.performSemanticsAction(SemanticsActions.RequestFocus)

        // Then
        onNodeWithText("Others with Clinic open:\n• Bob").assertIsDisplayed()

        // When a different KB is selected while someone is present there
        runOnIdle {
            selected.value = KBInfo("thyroids", "Thyroids")
            others.value = listOf("Carol")
        }

        // Then no old popup remains and the new indicator describes the selected KB
        onNodeWithText("Others with Clinic open:\n• Bob").assertDoesNotExist()
        presence.assertTextEquals("1").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, "Others with Thyroids open:\n• Carol"
        ))

        // When the KB is closed before a presence update arrives
        runOnIdle { selected.value = null }

        // Then
        presence.assertDoesNotExist()
    }
}
