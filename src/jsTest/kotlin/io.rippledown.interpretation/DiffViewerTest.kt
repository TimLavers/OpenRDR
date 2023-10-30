package io.rippledown.interpretation

import Api
import io.kotest.matchers.shouldBe
import io.rippledown.model.diff.*
import kotlinx.coroutines.MainScope
import mocks.config
import mocks.mock
import proxy.waitForEvents
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

class DiffViewerTest {

    @Test
    fun shouldNotShowAnyRowsIfNoChanges() {
        val fc = FC {
            DiffViewer {
                diffList = DiffList()
            }
        }
        runReactTest(fc) { container ->
            with(container) {
                requireNumberOfRows(0)
            }
        }
    }

    @Test
    fun shouldShowARowForEachUnchangedDiff() {
        val fc = FC {
            DiffViewer {
                diffList = DiffList(listOf(Unchanged(), Unchanged(), Unchanged()))
            }
        }
        runReactTest(fc) { container ->
            with(container) {
                requireNumberOfRows(3)
            }
        }

    }

    @Test
    fun shouldShowARowForEachChangedDiff() {
        val fc = FC {
            DiffViewer {
                diffList = DiffList(listOf(Addition(), Removal(), Replacement()))
            }
        }
        runReactTest(fc) { container ->
            with(container) {
                requireNumberOfRows(3)
            }
        }

    }

    @Test
    fun shouldShowABuildIconByDefaultForFirstUnchangedDiff() {
        val fc = FC {
            DiffViewer {
                diffList = DiffList(
                    listOf(
                        Unchanged(),
                        Unchanged(),
                        Unchanged(),
                        Addition(),
                        Unchanged()
                    )
                )
            }
        }
        runReactTest(fc) { container ->
            with(container) {
                requireBuildIconForRow(3)
            }
        }
    }

    @Test
    fun shouldShowABuildIconWhenMouseIsOverChangedDiff() {
        val fc = FC {
            DiffViewer {
                diffList = DiffList(
                    listOf(
                        Addition(),
                        Unchanged(),
                        Removal(),
                        Unchanged()
                    )
                )
            }
        }
        runReactTest(fc) { container ->
            with(container) {
                moveMouseOverRow(2)
                requireBuildIconForRow(2)
            }
        }
    }

    @Test
    fun shouldNotShowABuildIconWhenMouseIsOverUnchangedDiff() {
        val fc = FC {
            DiffViewer {
                diffList = DiffList(
                    listOf(
                        Addition(),
                        Unchanged(),
                        Removal(),
                        Unchanged()
                    )
                )
            }
        }
        runReactTest(fc) { container ->
            with(container) {
                moveMouseOverRow(1)
                requireNoBuildIconForRow(1)
            }
        }
    }

    @Test
    fun shouldShowAnUnchangedTextInOriginalAndChangedColumns() {
        val text = "Go to Bondi now!"
        val fc = FC {
            DiffViewer {
                diffList = DiffList(
                    listOf(
                        Unchanged(text),
                    )
                )
            }
        }
        runReactTest(fc) { container ->
            with(container) {
                requireOriginalTextInRow(0, text)
                requireChangedTextInRow(0, text)
            }
        }
    }

    @Test
    fun shouldShowAnAddedTextInGreenInChangedColumnOnly() {
        val text = "Go to Bondi now!"
        val fc = FC {
            DiffViewer {
                diffList = DiffList(diffs = listOf(Addition(text)))
            }
        }
        runReactTest(fc) { container ->
            with(container) {
                requireNoOriginalTextInRow(0)
                requireChangedTextInRow(0, text)
                requireGreenBackgroundInChangedColumnInRow(0)
            }
        }
    }

    @Test
    fun shouldShowARemovedTextInRedInOriginalColumnOnly() {
        val text = "Go to Bondi now!"
        val fc = FC {
            DiffViewer {
                diffList = DiffList(
                    listOf(
                        Removal(text),
                    )
                )
            }
        }
        runReactTest(fc) { container ->
            with(container) {
                requireOriginalTextInRow(0, text)
                requireNoChangedTextInRow(0)
                requireRedBackgroundInOriginalColumnInRow(0)
            }
        }
    }

    @Test
    fun shouldShowAReplacedAndReplacementTextsInTheirRespectiveColumnsWithCorrespondingColours() {
        val replaced = "Go to Bondi"
        val replacement = "Go to Bondi now!"
        val fc = FC {
            DiffViewer {
                diffList = DiffList(
                    listOf(
                        Replacement(replaced, replacement),
                    )
                )
            }
        }
        runReactTest(fc) { container ->
            with(container) {
                requireOriginalTextInRow(0, replaced)
                requireChangedTextInRow(0, replacement)
                requireRedBackgroundInOriginalColumnInRow(0)
                requireGreenBackgroundInChangedColumnInRow(0)
            }
        }
    }

    @Test
    fun shouldCallOnStartRuleWhenTheBuildIconIsClicked() {
        var ruleStarted = false
        val fc = FC {
            DiffViewer {
                scope = MainScope()
                api = Api(mock(config {}))
                diffList = DiffList(
                    listOf(
                        Addition("Go to Bondi now!"),
                        Unchanged(),
                        Removal(),
                    )
                )
                onStartRule = {
                    ruleStarted = true
                }
            }
        }
        runReactTest(fc) { container ->
            with(container) {
                ruleStarted shouldBe false
                requireBuildIconForRow(0)
                clickBuildIconForRow(0)
                ruleStarted shouldBe true
            }
        }
    }

    @Test
    fun onStartRuleShouldIdentifyTheSelectedDiff() {
        val differenceList = DiffList(
            listOf(
                Addition("Go to Bondi now!"),
                Unchanged("Enjoy the beach!"),
                Removal("Go to Manly now!"),
            )
        )
        val fc = FC {
            DiffViewer {
                scope = MainScope()
                diffList = differenceList
                onStartRule = { selectedDiff ->
                    selectedDiff shouldBe 2
                }
            }
        }
        runReactTest(fc) { container ->
            with(container) {
                moveMouseOverRow(2)
                waitForEvents()
                clickBuildIconForRow(2)
                //assertion is in onStartRule
            }
        }
    }
}