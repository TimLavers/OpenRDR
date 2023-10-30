package io.rippledown.kb

import proxy.waitForEvents
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Ignore
import kotlin.test.Test

@Ignore //TODO: Fix this test.
class KBImportDialogTest {

    @Test
    fun importDialogShouldNotBeShowingInitially() {
        val vfc = FC {
            KBImportDialog {
            }
        }
        runReactTest(vfc) { container ->
            with(container) {
                requireImportKBButtonToBeShowing()
                requireImportDialogToNotBeShowing()
            }
        }
    }

    @Test
    fun shouldShowDialogWhenImportButtonClicked() {
        val vfc = FC {
            KBImportDialog {
            }
        }
        runReactTest(vfc) { container ->
            with(container) {
                clickKBImport()
                waitForEvents()
                requireConfirmImportKBButtonToBeShowing()
            }
        }
    }
}