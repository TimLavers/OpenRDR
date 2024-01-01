package io.rippledown.appbar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.kotest.matchers.shouldBe
import io.rippledown.constants.main.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class CreateKBTest {

    private var handler = DummyCreateKBHandler()

    @get:Rule
    var composeTestRule = createComposeRule()

    @Before
    fun setup() {
        handler = DummyCreateKBHandler()
    }

    @Test
    fun `initial layout`() = runTest {
        with(composeTestRule) {
            setContent {
                CreateKB(handler)
            }
            waitUntilExactlyOneExists(hasText(CREATE_KB_NAME))
            waitUntilExactlyOneExists(hasTestTag(CREATE_KB_NAME_FIELD_ID))
            onNodeWithTag(CREATE_KB_NAME_FIELD_ID)
                .assertIsEnabled()
                .assertIsDisplayed()
                .assertTextEquals("")
            onNodeWithTag(CREATE_KB_OK_BUTTON_ID)
                .assertIsNotEnabled()
                .assertIsDisplayed()
                .assertTextEquals(OK)
            onNodeWithTag(CREATE_KB_CANCEL_BUTTON_ID)
                .assertIsEnabled()
                .assertIsDisplayed()
                .assertTextEquals(CANCEL)
        }
    }

    @Test
    fun cancel() = runTest {
        with(composeTestRule) {
            setContent {
                CreateKB(handler)
            }
            waitUntilExactlyOneExists(hasText(CREATE_KB_NAME))
            onNodeWithTag(CREATE_KB_CANCEL_BUTTON_ID).performClick()
            handler.cancelled shouldBe true
        }
    }

    @Test
    fun ok() = runTest {
        val newKBName = "Whatever"
        with(composeTestRule) {
            setContent {
                CreateKB(handler)
            }
            waitUntilExactlyOneExists(hasText(CREATE_KB_NAME))
            onNodeWithTag(CREATE_KB_NAME_FIELD_ID).performTextInput(newKBName)
            onNodeWithTag(CREATE_KB_OK_BUTTON_ID).performClick()
            handler.createdName shouldBe newKBName
        }
    }

    @Test
    fun `kb name validation`() = runTest {
        with(composeTestRule) {
            setContent {
                CreateKB(handler)
            }
            waitUntilExactlyOneExists(hasText(CREATE_KB_NAME))
            onNodeWithTag(CREATE_KB_NAME_FIELD_ID).performTextInput("A")
            onNodeWithTag(CREATE_KB_OK_BUTTON_ID).assertIsEnabled()
            onNodeWithTag(CREATE_KB_NAME_FIELD_ID).performTextClearance()
            onNodeWithTag(CREATE_KB_OK_BUTTON_ID).assertIsNotEnabled()
            onNodeWithTag(CREATE_KB_NAME_FIELD_ID).performTextInput("Bats")
            onNodeWithTag(CREATE_KB_OK_BUTTON_ID).assertIsEnabled()
        }
    }

    class DummyCreateKBHandler: CreateKBHandler {
        var createdName = ""
        var cancelled = false
        override fun create(name: String) {
            createdName = name
        }

        override fun cancel() {
            cancelled = true
        }
    }
}