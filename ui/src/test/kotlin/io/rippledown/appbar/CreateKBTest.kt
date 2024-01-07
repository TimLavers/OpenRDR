package io.rippledown.appbar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import io.kotest.matchers.shouldBe
import io.rippledown.constants.main.*
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class CreateKBTest {

    private var handler = DummyCreateKBHandler()

    @get:Rule
    var composeTestRule = createComposeRule()

    private lateinit var createKbOperator: CreateKbOperator

    @Before
    fun setup() {
        handler = DummyCreateKBHandler()
        composeTestRule.setContent {
            CreateKB(handler)
        }
        createKbOperator = CreateKbOperator(composeTestRule)
    }

    @Test
    fun `initial layout`() {
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasTestTag(CREATE_KB_NAME_FIELD_ID))
            onNodeWithTag(CREATE_KB_NAME_FIELD_ID)
                .assertIsEnabled()
                .assertIsDisplayed()
                .assertTextEquals("")
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
        handler.cancelled shouldBe false
        createKbOperator.clickCancelButton()
        handler.cancelled shouldBe true
    }

    @Test
    fun ok() {
        val newKBName = "Whatever"
        createKbOperator.performTextInput(newKBName)
        createKbOperator.clickCreateButton()
        handler.createdName shouldBe newKBName
        handler.cancelled shouldBe false
    }

    @Test
    fun `kb name validation`() {
        createKbOperator.performTextInput("A")
        createKbOperator.assertCreateButtonIsEnabled()
        createKbOperator.performTextClearance()
        createKbOperator.assertOkButtonIsNotEnabled()
        createKbOperator.performTextInput("Bats")
        createKbOperator.assertCreateButtonIsEnabled()
    }

    class DummyCreateKBHandler : CreateKBHandler {
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

@OptIn(ExperimentalTestApi::class)
class CreateKbOperator(private val composeTestRule: ComposeContentTestRule) {
    init {
        composeTestRule.waitUntilExactlyOneExists(hasText(CREATE_KB_NAME))
    }

    fun setNameAndClickCreate(text: String) {
        performTextInput(text)
        clickCreateButton()
    }

    fun waitToVanish() {
        composeTestRule.waitUntil {
            composeTestRule.onAllNodesWithTag(CREATE_KB_NAME_FIELD_ID).fetchSemanticsNodes().isEmpty()
        }
    }

    fun performTextInput(text: String) = composeTestRule.onNodeWithTag(CREATE_KB_NAME_FIELD_ID).performTextInput(text)

    fun performTextClearance() = composeTestRule.onNodeWithTag(CREATE_KB_NAME_FIELD_ID).performTextClearance()

    fun assertCreateButtonIsEnabled() = composeTestRule.onNodeWithTag(CREATE_KB_OK_BUTTON_ID).assertIsEnabled()

    fun assertOkButtonIsNotEnabled() = composeTestRule.onNodeWithTag(CREATE_KB_OK_BUTTON_ID).assertIsNotEnabled()

    fun clickCreateButton() = composeTestRule.onNodeWithTag(CREATE_KB_OK_BUTTON_ID).performClick()

    fun clickCancelButton() = composeTestRule.onNodeWithTag(CREATE_KB_CANCEL_BUTTON_ID).performClick()
}