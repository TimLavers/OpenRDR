package io.rippledown.appbar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import io.kotest.matchers.shouldBe
import io.rippledown.constants.kb.KB_CONTROL_ID
import io.rippledown.constants.main.CREATE_KB_ITEM_ID
import io.rippledown.constants.main.CREATE_KB_TEXT
import io.rippledown.main.Handler
import io.rippledown.main.handlerImpl
import io.rippledown.mocks.engineConfig
import io.rippledown.model.KBInfo
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import javax.swing.SwingUtilities
import kotlin.test.Test

class KBControlTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var uiKbControlOperator: KbControlOperator

    @Before
    fun setup() {
        composeTestRule.setContent {
            ApplicationBar(object : Handler by handlerImpl, AppBarHandler {
                override var isRuleSessionInProgress = false
            })
        }
        uiKbControlOperator = KbControlOperator(composeTestRule)
    }

    @Test
    fun `should select default project`() {
        uiKbControlOperator.assertKbNameIs(engineConfig.returnKBInfo.name)
    }

    @Test
    fun `create KB`() = runTest {
        val newKbName = "Lipids"
        engineConfig.returnKBInfo = KBInfo("12345_id", newKbName)
        uiKbControlOperator.assertCreateKbButtonIsNotShowing()
        uiKbControlOperator.clickControl()

        uiKbControlOperator.assertCreateKbButtonIsShowing()
        val uiCreateKB = uiKbControlOperator.clickCreateKbButton()
        uiCreateKB.assertOkButtonIsNotEnabled()
        SwingUtilities.invokeAndWait(Runnable {
            uiCreateKB.setNameAndClickCreate(newKbName)
        })
        engineConfig.newKbName shouldBe  newKbName
//        uiCreateKB.waitToVanish()

    }
}

@OptIn(ExperimentalTestApi::class)
class KbControlOperator(private val composeTestRule: ComposeContentTestRule) {
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

    fun clickCreateKbButton(): CreateKbOperator {
        composeTestRule.onNodeWithTag(CREATE_KB_ITEM_ID).performClick()
        return CreateKbOperator(composeTestRule)
    }
}
