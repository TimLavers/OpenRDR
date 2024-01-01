package io.rippledown.appbar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.InternalPlatformDsl.toStr
import io.rippledown.constants.kb.KB_SELECTOR_ID
import io.rippledown.constants.main.CREATE_KB_TEXT
import io.rippledown.constants.main.KBS_DROPDOWN_ID
import io.rippledown.main.Handler
import io.rippledown.main.handlerImpl
import io.rippledown.mocks.engineConfig
import io.rippledown.proxy.findById
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class KBControlTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should select default project`() = runTest {
        with(composeTestRule) {
            setContent {
                ApplicationBar(object : Handler by handlerImpl, AppBarHandler {
                    override var isRuleSessionInProgress = false
                })
            }
            waitUntilExactlyOneExists(hasText(engineConfig.returnKBInfo.name))
        }
    }

    @Test
    fun `create KB button`() = runTest {
        with(composeTestRule) {
            setContent {
                ApplicationBar(object : Handler by handlerImpl, AppBarHandler {
                    override var isRuleSessionInProgress = false
                })
            }
            waitUntilExactlyOneExists(hasText(engineConfig.returnKBInfo.name))
            onNodeWithText(engineConfig.returnKBInfo.name).performClick()
            waitUntilExactlyOneExists(hasText(CREATE_KB_TEXT))
            onNodeWithText(CREATE_KB_TEXT).performClick()

//            onNodeWithText(engineConfig.returnKBInfo.name).as()
//            onNodeWithTag(KB_SELECTOR_ID).assertExists()
//            waitUntilExactlyOneExists(hasTestTag(KB_SELECTOR_ID))
        }

    }
}