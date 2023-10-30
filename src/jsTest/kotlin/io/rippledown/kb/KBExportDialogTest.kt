package io.rippledown.kb

import react.FC
import react.dom.test.runReactTest
import kotlin.test.Ignore

@Ignore //TODO: Fix this test.
class KBExportDialogTest {

    fun exportDialogShouldNotBeShowingInitially() {
        val vfc = FC {
            KBImportDialog {
            }
        }
        runReactTest(vfc) { container ->
            with(container) {
                requireExportKBButtonToBeShowing()
                requireExportDialogToNotBeShowing()
            }
        }
    }
}