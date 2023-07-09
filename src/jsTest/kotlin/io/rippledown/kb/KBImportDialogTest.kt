package io.rippledown.kb

import kotlinx.coroutines.test.runTest
import proxy.waitForEvents
import react.FC
import react.dom.createRootFor
import kotlin.test.Ignore
import kotlin.test.Test

@Ignore //TODO: Fix this test.
class KBImportDialogTest {

    @Test
    fun importDialogShouldNotBeShowingInitially() = runTest {
        val vfc = FC {
            KBImportDialog {
            }
        }
        with(createRootFor(vfc)) {
            requireImportKBButtonToBeShowing()
            requireImportDialogToNotBeShowing()
        }
    }

    @Test
    fun shouldShowDialogWhenImportButtonClicked() = runTest {
        val vfc = FC {
            KBImportDialog {
            }
        }
        with(createRootFor(vfc)) {
            clickKBImport()
            waitForEvents()
            requireConfirmImportKBButtonToBeShowing()
        }
    }
}