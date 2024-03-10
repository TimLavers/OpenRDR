package io.rippledown.appbar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.constants.main.*
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class CreateKBTest {

    private lateinit var handler: CreateKBHandler

    @get:Rule
    var composeTestRule = createComposeRule()

    @Before
    fun setup() {
        handler = mockk<CreateKBHandler>(relaxed = true)
    }


    @Test
    fun `initial layout`() {
        with(composeTestRule) {
            setContent {
                CreateKB(handler)
            }
            waitUntilExactlyOneExists(hasTestTag(CREATE_KB_NAME_FIELD_ID))
            onNodeWithTag(CREATE_KB_NAME_FIELD_ID)
                .assertIsEnabled()
                .assertIsDisplayed()
            waitUntilExactlyOneExists(hasText(""))

            onNodeWithTag(CREATE_KB_OK_BUTTON_ID)
                .assertIsNotEnabled()
                .assertIsDisplayed()
                .assertTextEquals(CREATE)
            onNodeWithTag(CREATE_KB_CANCEL_BUTTON_ID)
                .assertIsEnabled()
                .assertIsDisplayed()
                .assertTextEquals(CANCEL)
        }
    }

    @Test
    fun cancel() {
        with(composeTestRule) {
            setContent {
                CreateKB(handler)
            }
            clickCancelButton()

            verify { handler.cancel() }
        }
    }

    @Test
    fun ok() {
        with(composeTestRule) {
            setContent {
                CreateKB(handler)
            }
            val newKBName = "Whatever"
            enterKBName(newKBName)
            clickCreateButton()
            verify { handler.create(newKBName) }
            verify { handler.cancel wasNot Called }
        }
    }

    @Test
    fun `kb name validation`() {
        with(composeTestRule) {
            setContent {
                CreateKB(handler)
            }
            val createKbOperator = onNodeWithTag(CREATE_KB_OK_BUTTON_ID)
            createKbOperator.assertIsNotEnabled()
            enterKBName("A")
            createKbOperator.assertIsEnabled()
            performTextClearance()
            createKbOperator.assertIsNotEnabled()
            enterKBName("Bats")
            createKbOperator.assertIsEnabled()
        }
    }
}


fun main() {

    application {
        Window(
            onCloseRequest = ::exitApplication,
        ) {
            CreateKB(object : CreateKBHandler {
                override var create: (name: String) -> Unit = { name -> }
                override var cancel: () -> Unit = {}
            })
        }
    }
}
