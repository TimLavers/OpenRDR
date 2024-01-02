package io.rippledown.appbar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import io.rippledown.constants.kb.KB_CONTROL_ID
import io.rippledown.constants.main.CREATE_KB_ITEM_ID
import io.rippledown.constants.main.CREATE_KB_TEXT
import io.rippledown.main.Handler
import io.rippledown.main.handlerImpl
import io.rippledown.mocks.engineConfig
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class KBControlTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var uiKbControl: UIKBControl

    @Before
    fun setup() {
        composeTestRule.setContent {
            ApplicationBar(object : Handler by handlerImpl, AppBarHandler {
                override var isRuleSessionInProgress = false
            })
        }
        uiKbControl = UIKBControl(composeTestRule)
    }

    @Test
    fun `should select default project`() {
        uiKbControl.assertKbNameIs(engineConfig.returnKBInfo.name)
    }

    @Test
    fun `create KB button`() {
        uiKbControl.assertCreateKbButtonIsNotShowing()
        uiKbControl.clickControl()

        uiKbControl.assertCreateKbButtonIsShowing()
        val uiCreateKB = uiKbControl.clickCreateKbButton()
        uiCreateKB.assertOkButtonIsNotEnabled()
    }
}

@OptIn(ExperimentalTestApi::class)
class UIKBControl(private val composeTestRule: ComposeContentTestRule) {
    init {
        composeTestRule.waitUntilExactlyOneExists(hasTestTag(KB_CONTROL_ID))
    }

    fun assertKbNameIs(expected: String) {
        composeTestRule.waitUntilExactlyOneExists(hasText(expected))
        val onNodeWithTag = composeTestRule.onNodeWithTag(KB_CONTROL_ID)
        onNodeWithTag.assertTextEquals(expected)
    }

    fun clickControl() = composeTestRule.onNodeWithTag(KB_CONTROL_ID).performClick()

    fun assertCreateKbButtonIsNotShowing() = composeTestRule.onAllNodesWithText(CREATE_KB_TEXT).assertCountEquals(0)

    fun assertCreateKbButtonIsShowing() {
        composeTestRule.waitUntilExactlyOneExists(hasText(CREATE_KB_TEXT))
        composeTestRule.onNodeWithText(CREATE_KB_TEXT).assertIsEnabled()
    }

    fun clickCreateKbButton(): UICreateKB {
        composeTestRule.onNodeWithTag(CREATE_KB_ITEM_ID).performClick()
        return UICreateKB(composeTestRule)
    }
}
