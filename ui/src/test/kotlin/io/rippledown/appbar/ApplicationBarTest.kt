package io.rippledown.appbar

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.rippledown.constants.kb.KB_CONTROL_CURRENT_KB_LABEL_DESCRIPTION
import io.rippledown.constants.kb.KB_NAME_ID
import io.rippledown.constants.kb.NO_KB_SELECTED
import io.rippledown.model.KBInfo
import org.junit.Rule
import kotlin.test.Test

class ApplicationBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    private val bondiInfo = KBInfo("Bondi")

    @Test
    fun `KB label is always read only with no menu`() {
        // Given
        val selected = mutableStateOf<KBInfo?>(null)
        composeTestRule.setContent { ApplicationBar(selected.value) }

        // When
        val label = composeTestRule.onNodeWithTag(KB_NAME_ID, useUnmergedTree = true)

        // Then
        label.assertTextEquals(NO_KB_SELECTED).assertHasNoClickAction()
        composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(0)
        composeTestRule.runOnIdle { selected.value = bondiInfo }
        label.assertTextEquals(bondiInfo.name).assertHasNoClickAction()
        composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(0)
        composeTestRule.runOnIdle { selected.value = null }
        label.assertTextEquals(NO_KB_SELECTED).assertHasNoClickAction()
    }

    @Test
    fun `should show current KB name`() {
        // Given
        composeTestRule.setContent { ApplicationBar(bondiInfo) }

        // When
        val label = composeTestRule.onNodeWithTag(KB_NAME_ID, useUnmergedTree = true)

        // Then
        label.assertTextEquals(bondiInfo.name)
    }

    @Test
    fun semantics() {
        // Given
        composeTestRule.setContent { ApplicationBar(bondiInfo) }

        // When
        val label = composeTestRule.onNodeWithContentDescription(KB_CONTROL_CURRENT_KB_LABEL_DESCRIPTION)

        // Then
        label.assertExists()
    }
}
