package io.rippledown.kb

import kotlinx.coroutines.test.TestResult
import proxy.waitForEvents
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Ignore
import kotlin.test.Test

@Ignore //TODO: Fix this test.
class KBImportDialogTest {

    @Test
    fun importDialogShouldNotBeShowingInitially(): TestResult {
        val vfc = FC {
            ImportKB {
            }
        }
        return runReactTest(vfc) { container ->
            with(container) {
                requireImportKBButtonToBeShowing()
                requireImportDialogToNotBeShowing()
            }
        }
    }

    @Test
    fun shouldShowDialogWhenImportButtonClicked(): TestResult {
        val vfc = FC {
            ImportKB {
            }
        }
        return runReactTest(vfc) { container ->
            with(container) {
                clickKBImport()
                waitForEvents()
                requireConfirmImportKBButtonToBeShowing()
            }
        }
    }
}