package io.rippledown.appbar

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import io.rippledown.constants.kb.PRESENCE_DESCRIPTION
import io.rippledown.model.KBInfo
import org.junit.jupiter.api.Test

@OptIn(ExperimentalTestApi::class)
class ApplicationBarPresenceTest {
    @Test
    fun `presence is absent when alone and joins the other users names`() = runComposeUiTest {
        // Given
        val others = mutableStateOf(emptyList<String>())
        setContent { ApplicationBar(KBInfo("Clinic"), othersPresent = others.value) }
        val presence = onNodeWithContentDescription(PRESENCE_DESCRIPTION)
        presence.assertDoesNotExist()

        // When
        runOnIdle { others.value = listOf("Bob", "Carol") }

        // Then
        presence.assertIsDisplayed().assertTextEquals("Also here: Bob, Carol").assertHasNoClickAction()

        // When
        runOnIdle { others.value = listOf("Bob") }

        // Then
        presence.assertTextEquals("Also here: Bob")

        // When
        runOnIdle { others.value = emptyList() }

        // Then
        presence.assertDoesNotExist()
    }
}
