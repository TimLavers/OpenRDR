package io.rippledown.kb

import kotlinx.coroutines.test.TestResult
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Ignore
import kotlin.test.Test

@Ignore //TODO: Fix this test.
class KBExportDialogTest {

    @Test
    fun exportDialogShouldNotBeShowingInitially(): TestResult {
        val vfc = FC {
            KBImportDialog {
            }
        }
        return runReactTest(vfc) { container ->
            with(container) {
                requireExportKBButtonToBeShowing()
                requireExportDialogToNotBeShowing()
            }
        }
    }
}