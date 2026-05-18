package io.rippledown.casecontrol

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.kotest.matchers.shouldBe
import io.rippledown.constants.caseview.CASE_VIEW_FILTER_CLEAR_DESCRIPTION
import io.rippledown.constants.caseview.CASE_VIEW_FILTER_FIELD_DESCRIPTION
import io.rippledown.constants.caseview.CASE_VIEW_FILTER_PLACEHOLDER
import org.junit.Rule
import kotlin.test.Test

/**
 * Unit tests for [CaseViewFilterField] in isolation. The integration of this
 * widget with the case panels is covered by `CaseControlTest`; here we pin
 * down the widget's own contract:
 *  - the placeholder is shown only while the value is empty
 *  - typing is delivered to the caller via [onValueChange]
 *  - the clear button is shown only while the value is non-empty
 *  - clicking the clear button invokes [onClear]
 */
@OptIn(ExperimentalTestApi::class)
class CaseViewFilterFieldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `placeholder is shown when the value is empty`() {
        with(composeTestRule) {
            setContent {
                CaseViewFilterField(value = "", onValueChange = {}, onClear = {})
            }
            onNodeWithText(CASE_VIEW_FILTER_PLACEHOLDER).assertExists()
        }
    }

    @Test
    fun `placeholder is hidden when the value is non-empty`() {
        with(composeTestRule) {
            setContent {
                CaseViewFilterField(value = "abc", onValueChange = {}, onClear = {})
            }
            onNodeWithText(CASE_VIEW_FILTER_PLACEHOLDER).assertDoesNotExist()
        }
    }

    @Test
    fun `typing into the field invokes onValueChange with each new value`() {
        var captured = ""
        var current by mutableStateOf("")
        with(composeTestRule) {
            setContent {
                CaseViewFilterField(
                    value = current,
                    onValueChange = {
                        captured = it
                        current = it
                    },
                    onClear = {}
                )
            }
            onNodeWithContentDescription(CASE_VIEW_FILTER_FIELD_DESCRIPTION).performTextInput("hgb")
            captured shouldBe "hgb"
        }
    }

    @Test
    fun `the field renders the value supplied by the caller`() {
        with(composeTestRule) {
            setContent {
                CaseViewFilterField(value = "100.2", onValueChange = {}, onClear = {})
            }
            onNodeWithContentDescription(CASE_VIEW_FILTER_FIELD_DESCRIPTION)
                .assertTextEquals("100.2")
        }
    }

    @Test
    fun `the clear button is not rendered when the value is empty`() {
        with(composeTestRule) {
            setContent {
                CaseViewFilterField(value = "", onValueChange = {}, onClear = {})
            }
            onNodeWithContentDescription(CASE_VIEW_FILTER_CLEAR_DESCRIPTION).assertDoesNotExist()
        }
    }

    @Test
    fun `the clear button is rendered when the value is non-empty`() {
        with(composeTestRule) {
            setContent {
                CaseViewFilterField(value = "x", onValueChange = {}, onClear = {})
            }
            onNodeWithContentDescription(CASE_VIEW_FILTER_CLEAR_DESCRIPTION).assertExists()
        }
    }

    @Test
    fun `clicking the clear button invokes onClear`() {
        var clearedTimes = 0
        with(composeTestRule) {
            setContent {
                CaseViewFilterField(value = "abc", onValueChange = {}, onClear = { clearedTimes++ })
            }
            onNodeWithContentDescription(CASE_VIEW_FILTER_CLEAR_DESCRIPTION).performClick()
        }
        clearedTimes shouldBe 1
    }

    @Test
    fun `pressing Escape on a non-empty field invokes onClear`() {
        // Escape clearing requires the field to have focus (the production
        // handler is wired via Modifier.onPreviewKeyEvent on the BasicTextField),
        // so we type a character first to focus the field, then press Escape.
        var clearedTimes = 0
        var current by mutableStateOf("")
        with(composeTestRule) {
            setContent {
                CaseViewFilterField(
                    value = current,
                    onValueChange = { current = it },
                    onClear = {
                        clearedTimes++
                        current = ""
                    }
                )
            }
            onNodeWithContentDescription(CASE_VIEW_FILTER_FIELD_DESCRIPTION).performTextInput("a")
            onNodeWithContentDescription(CASE_VIEW_FILTER_FIELD_DESCRIPTION).performKeyInput {
                pressKey(Key.Escape)
            }
        }
        clearedTimes shouldBe 1
        current shouldBe ""
    }

    @Test
    fun `pressing Escape on an empty field does not invoke onClear`() {
        // Defensive: Escape on an empty field must NOT consume the event,
        // so dialogs/menus that listen for Escape upstream still get it.
        var clearedTimes = 0
        var current by mutableStateOf("a")
        with(composeTestRule) {
            setContent {
                CaseViewFilterField(
                    value = current,
                    onValueChange = { current = it },
                    onClear = { clearedTimes++ }
                )
            }
            // Focus the field by typing, then clear the value via the caller
            // and finally press Escape: the field is now focused but empty.
            onNodeWithContentDescription(CASE_VIEW_FILTER_FIELD_DESCRIPTION).performTextInput("b")
            current = ""
            composeTestRule.waitForIdle()
            onNodeWithContentDescription(CASE_VIEW_FILTER_FIELD_DESCRIPTION).performKeyInput {
                pressKey(Key.Escape)
            }
        }
        clearedTimes shouldBe 0
    }

    @Test
    fun `replacing the value via the caller is reflected by the field`() {
        var current by mutableStateOf("hgb")
        with(composeTestRule) {
            setContent {
                CaseViewFilterField(
                    value = current,
                    onValueChange = { current = it },
                    onClear = { current = "" }
                )
            }
            onNodeWithContentDescription(CASE_VIEW_FILTER_FIELD_DESCRIPTION)
                .assertTextEquals("hgb")
            onNodeWithContentDescription(CASE_VIEW_FILTER_FIELD_DESCRIPTION)
                .performTextReplacement("mcv")
            current shouldBe "mcv"
        }
    }
}
