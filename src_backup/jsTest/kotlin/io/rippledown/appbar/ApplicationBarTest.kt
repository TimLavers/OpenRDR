package io.rippledown.appbar

import io.kotest.matchers.shouldBe
import io.rippledown.constants.main.MAIN_HEADING
import io.rippledown.constants.main.MAIN_HEADING_ID
import io.rippledown.kb.requireKBControlsToBeDisabled
import io.rippledown.kb.requireKBControlsToBeEnabled
import io.rippledown.kb.requireKBName
import io.rippledown.main.Api
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.test.TestResult
import mocks.EngineConfig
import mocks.defaultMock
import proxy.findById
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

class ApplicationBarTest {

    @Test
    fun shouldShowHeading(): TestResult {
        val fc = FC {
            ApplicationBar {
                scope = MainScope()
                api = Api(defaultMock)
            }
        }
        return runReactTest(fc) { container ->
            container.findById(MAIN_HEADING_ID).textContent shouldBe MAIN_HEADING
        }
    }

    @Test
    fun shouldSelectDefaultProject(): TestResult {
        val fc = FC {
            ApplicationBar {
                scope = MainScope()
                api = Api(defaultMock)
            }
        }
        return runReactTest(fc) { container ->
            container.requireKBName(EngineConfig().returnKBInfo.name)
        }
    }

    @Test
    fun enableKBControlsByDefault(): TestResult {
        val fc = FC {
            ApplicationBar {
                scope = MainScope()
                api = Api(defaultMock)
                isRuleSessionInProgress = false
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                requireKBControlsToBeEnabled()
            }
        }
    }

    @Test
    fun disableKBControlsWhenRuleBuilding(): TestResult {
        val fc = FC {
            ApplicationBar {
                scope = MainScope()
                api = Api(defaultMock)
                isRuleSessionInProgress = true
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                requireKBControlsToBeDisabled()
            }
        }
    }
}
