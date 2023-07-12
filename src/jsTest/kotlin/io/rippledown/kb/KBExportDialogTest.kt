package io.rippledown.kb

import kotlinx.coroutines.test.runTest
import react.FC
import react.dom.createRootFor
import kotlin.test.Ignore

@Ignore //TODO: Fix this test.
class KBExportDialogTest {

    fun exportDialogShouldNotBeShowingInitially() = runTest {
        val vfc = FC {
            KBImportDialog {
            }
        }
        with(createRootFor(vfc)) {
            requireExportKBButtonToBeShowing()
            requireExportDialogToNotBeShowing()
        }
    }
}