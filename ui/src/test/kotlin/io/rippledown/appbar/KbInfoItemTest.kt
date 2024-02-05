package io.rippledown.appbar

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.matchers.shouldBe
import io.rippledown.model.KBInfo
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class KbInfoItemTest {

    private val kbInfo = KBInfo("gl123", "Glucose")
    private var selected = false
    private val handler = object: KbSelectionHandler{
        override fun kbInfo() = kbInfo

        override fun select() {
            selected = true
        }
    }

    @get:Rule
    var composeTestRule = createComposeRule()

    @Before
    fun setup() {
        selected = false
        composeTestRule.setContent {
            KbInfoItem(handler)
        }
    }

    @Test
    fun `item text`() {
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText(kbInfo.name))
        }
    }

    @Test
    fun select() {
        with(composeTestRule) {
            selected shouldBe false
            onNodeWithText(kbInfo.name).performClick()
            selected shouldBe true
        }
    }
}