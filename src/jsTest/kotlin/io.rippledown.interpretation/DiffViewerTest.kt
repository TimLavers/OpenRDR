package io.rippledown.interpretation

import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Removal
import io.rippledown.model.diff.Replacement
import io.rippledown.model.diff.Unchanged
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import react.VFC
import react.dom.checkContainer
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiffViewerTest {

    @Test
    fun shouldShowTheTitle() = runTest {
        val vfc = VFC {
            DiffViewer {
                changes = listOf()
            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                requireTitle("Changes")
            }
        }
    }

    @Test
    fun shouldNotShowAnyRowsIfNoChanges() = runTest {
        val vfc = VFC {
            DiffViewer {
                changes = listOf()
            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                requireNumberOfRows(0)
            }
        }

    }

    @Test
    fun shouldShowARowForEachUnchangedDiff() = runTest {
        val vfc = VFC {
            DiffViewer {
                changes = listOf(Unchanged(), Unchanged(), Unchanged())
            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                requireNumberOfRows(3)
            }
        }

    }

    @Test
    fun shouldShowARowForEachChangedDiff() = runTest {
        val vfc = VFC {
            DiffViewer {
                changes = listOf(Addition(), Removal(), Replacement())
            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                requireNumberOfRows(3)
            }
        }

    }

    @Test
    fun shouldShowACheckboxForAllDiffsExceptUnchanged() = runTest {
        val vfc = VFC {
            DiffViewer {
                changes = listOf(
                    Unchanged(),
                    Addition(),
                    Replacement(),
                    Removal(),
                    Unchanged()
                )
            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                requireNoCheckBoxForRow(0)
                requireCheckBoxForRow(1)
                requireCheckBoxForRow(2)
                requireCheckBoxForRow(3)
                requireNoCheckBoxForRow(4)
            }
        }
    }

    @Test
    fun shouldShowAnUnchangedTextInOriginalAndChangedColumns() = runTest {
        val text = "Go to Bondi now!"
        val vfc = VFC {
            DiffViewer {
                changes = listOf(
                    Unchanged(text),
                )
            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                requireOriginalTextInRow(0, text)
                requireChangedTextInRow(0, text)
            }
        }
    }

    @Test
    fun shouldShowAnAddedTextInGreenInChangedColumnOnly() = runTest {
        val text = "Go to Bondi now!"
        val vfc = VFC {
            DiffViewer {
                changes = listOf(
                    Addition(text),
                )
            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                requireNoOriginalTextInRow(0)
                requireChangedTextInRow(0, text)
                requireGreenBackgroundInChangedColumnInRow(0)
            }
        }
    }

    @Test
    fun shouldShowARemovedTextInRedInOriginalColumnOnly() = runTest {
        val text = "Go to Bondi now!"
        val vfc = VFC {
            DiffViewer {
                changes = listOf(
                    Removal(text),
                )
            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                requireOriginalTextInRow(0, text)
                requireNoChangedTextInRow(0)
                requireRedBackgroundInOriginalColumnInRow(0)
            }
        }
    }


    @Test
    fun shouldShowAReplacedAndReplacementTextsInTheirRespectiveColumnsWithCorrespondingColours() = runTest {
        val replaced = "Go to Bondi"
        val replacement = "Go to Bondi now!"
        val vfc = VFC {
            DiffViewer {
                changes = listOf(
                    Replacement(replaced, replacement),
                )
            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                requireOriginalTextInRow(0, replaced)
                requireChangedTextInRow(0, replacement)
                requireRedBackgroundInOriginalColumnInRow(0)
                requireGreenBackgroundInChangedColumnInRow(0)
            }
        }
    }
}