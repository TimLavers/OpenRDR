package io.rippledown.kb

import kotlinx.coroutines.test.runTest
import react.VFC
import react.dom.createRootFor
import kotlin.test.Ignore

@Ignore //TODO: Fix this test.
class KBExportDialogTest {

    fun exportDialogShouldNotBeShowingInitially() = runTest {
        val vfc = VFC {
            KBImportDialog {
            }
        }
        with(createRootFor(vfc)) {
            requireExportKBButtonToBeShowing()
            requireExportDialogToNotBeShowing()
        }
    }
}