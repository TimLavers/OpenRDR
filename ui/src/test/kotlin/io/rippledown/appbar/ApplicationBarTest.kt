package io.rippledown.appbar

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.printToString
import io.rippledown.constants.kb.KB_SELECTOR_ID
import io.rippledown.constants.main.MAIN_HEADING_ID
import io.rippledown.main.Handler
import io.rippledown.main.handlerImpl
import org.junit.Rule
import javax.accessibility.AccessibleRole
import kotlin.test.Test

class ApplicationBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should show application heading`() {
        with(composeTestRule) {
            setContent {
                ApplicationBar(object : Handler by handlerImpl, AppBarHandler {
                    override var isRuleSessionInProgress = false
                })
            }
            onNodeWithTag(testTag = MAIN_HEADING_ID).assertExists()
        }
    }

    @Test
    fun `should show KB selector if not rule building`() {
        with(composeTestRule) {
            setContent {
                ApplicationBar(object : Handler by handlerImpl, AppBarHandler {
                    override var isRuleSessionInProgress = false
                })
            }
            onNodeWithTag(testTag = KB_SELECTOR_ID, useUnmergedTree = true).assertExists()
        }
    }

    @Test
    fun `should remove KB selector if rule building`() {
        with(composeTestRule) {
            setContent {
                ApplicationBar(object : Handler by handlerImpl, AppBarHandler {
                    override var isRuleSessionInProgress = true
                })
            }
            onNodeWithTag(testTag = KB_SELECTOR_ID).assertDoesNotExist()
        }
    }

    @Test
    fun semantics() {
        with(composeTestRule) {
            setContent {
                ApplicationBar(object : Handler by handlerImpl, AppBarHandler {
                    override var isRuleSessionInProgress = false
                })
            }
            onRoot().printToLog(AccessibleRole.GROUP_BOX.toString())
        }
    }
}