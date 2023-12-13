package io.rippledown.appbar

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import io.rippledown.main.Handler
import io.rippledown.main.handlerImpl
import io.rippledown.mocks.engineConfig
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

class KBControlTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalTestApi::class)
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
}