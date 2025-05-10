package io.rippledown.appbar

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import io.kotest.matchers.shouldBe
import io.rippledown.constants.kb.CONFIRM_UNDO_LAST_RULE_TEXT
import io.rippledown.model.rule.UndoRuleDescription
import io.rippledown.utils.applicationFor
import org.junit.Rule
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class UndoRuleDescriptionDisplayTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    private val descriptionText = "This is the last rule!\n" +
            "It replaces the comment: \"Dogs have four legs and a long tail and other body parts.\"\n" +
            "with:\n" +
            "\"Cats have eight legs and thirteen tails, according to ChatGPT.\""
    val lastRuleDescription = UndoRuleDescription(descriptionText, true)
    var closeClicked = false
    var undone = false
    val handler = object : UndoRuleDescriptionDisplayHandler {
        override fun description() = lastRuleDescription

        override fun cancel() {
            closeClicked = true
        }

        override fun undoLastRule() {
            undone = true
        }
    }

    @BeforeTest
    fun setup() {
        closeClicked = false
        undone = false
    }

    @Test
    fun `should show rule description`() {
        with(composeTestRule) {
            setContent {
                UndoRuleDescriptionDisplay(handler)
            }
            waitUntilExactlyOneExists(hasText(descriptionText))
        }
    }

    @Test
    fun close() {
        with(composeTestRule) {
            setContent {
                UndoRuleDescriptionDisplay(handler)
            }
            closeClicked shouldBe false
            cancelShowLastRule()
            closeClicked shouldBe true
        }
    }

    @Test
    fun `click undo last rule then cancel`() {
        with(composeTestRule) {
            setContent {
                UndoRuleDescriptionDisplay(handler)
            }
            assertUndoLastRuleButtonIsShowing()
            clickUndoLastRule()
            waitUntilExactlyOneExists(hasText(CONFIRM_UNDO_LAST_RULE_TEXT))
            assertUndoLastRuleButtonIsNotShowing()
            clickUndoLastRuleConfirmationNoButton()
            assertUndoLastRuleButtonIsShowing()
            undone shouldBe false
            cancelShowLastRule()
            closeClicked shouldBe true
            undone shouldBe false
        }
    }

    @Test
    fun `click undo last rule then confirm`() {
        with(composeTestRule) {
            setContent {
                UndoRuleDescriptionDisplay(handler)
            }
            assertUndoLastRuleButtonIsShowing()
            clickUndoLastRule()
            waitUntilExactlyOneExists(hasText(CONFIRM_UNDO_LAST_RULE_TEXT))
            assertUndoLastRuleButtonIsNotShowing()
            undone shouldBe false
            clickUndoLastRuleConfirmationYesButton()
            undone shouldBe true
            assertUndoLastRuleButtonIsNotShowing()
        }
    }
}
fun main() {
    val descriptionText = "This is the last rule!\n" +
            "It replaces the comment: \"Dogs have four legs and a long tail and other body parts.\"\n" +
            "with:\n" +
            "\"Cats have eight legs and thirteen tails, according to ChatGPT.\""
    val lastRuleDescription = UndoRuleDescription(descriptionText, true)
    val handler = object : UndoRuleDescriptionDisplayHandler {
        override fun description() = lastRuleDescription

        override fun cancel() {
            print("Canceled")
        }

        override fun undoLastRule() {
            print("Undone!")
        }
    }
    applicationFor {
        UndoRuleDescriptionDisplay(handler)
    }
}