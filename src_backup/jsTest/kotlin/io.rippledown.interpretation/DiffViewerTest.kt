package io.rippledown.interpretation

import io.kotest.matchers.shouldBe
import io.rippledown.main.Api
import io.rippledown.model.diff.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.test.TestResult
import mocks.config
import mocks.mock
import proxy.waitForEvents
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

class DiffViewerTest {

    @Test
    fun shouldNotShowAnyRowsIfNoChanges(): TestResult {
        val fc = FC {
            DiffViewer {
                diffList = DiffList()
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                requireNumberOfRows(0)
            }
        }
    }

    @Test
    fun shouldShowARowForEachUnchangedDiff(): TestResult {
        val fc = FC {
            DiffViewer {
                diffList = DiffList(listOf(Unchanged(), Unchanged(), Unchanged()))
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                requireNumberOfRows(3)
            }
        }

    }

    @Test
    fun shouldShowARowForEachChangedDiff(): TestResult {
        val fc = FC {
            DiffViewer {
                diffList = DiffList(listOf(Addition(), Removal(), Replacement()))
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                requireNumberOfRows(3)
            }
        }

    }

    @Test
    fun shouldShowABuildIconByDefaultForFirstUnchangedDiff(): TestResult {
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
        return runReactTest(fc) { container ->
            with(container) {
                requireBuildIconForRow(3)
            }
        }
    }

    @Test
    fun shouldShowABuildIconWhenMouseIsOverChangedDiff(): TestResult {
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
        return runReactTest(fc) { container ->
            with(container) {
                moveMouseOverRow(2)
                requireBuildIconForRow(2)
            }
        }
    }

    @Test
    fun shouldNotShowABuildIconWhenMouseIsOverUnchangedDiff(): TestResult {
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
        return runReactTest(fc) { container ->
            with(container) {
                moveMouseOverRow(1)
                requireNoBuildIconForRow(1)
            }
        }
    }

    @Test
    fun shouldShowAnUnchangedTextInOriginalAndChangedColumns(): TestResult {
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
        return runReactTest(fc) { container ->
            with(container) {
                requireOriginalTextInRow(0, text)
                requireChangedTextInRow(0, text)
            }
        }
    }

    @Test
    fun shouldShowAnAddedTextInGreenInChangedColumnOnly(): TestResult {
        val text = "Go to Bondi now!"
        val fc = FC {
            DiffViewer {
                diffList = DiffList(diffs = listOf(Addition(text)))
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                requireNoOriginalTextInRow(0)
                requireChangedTextInRow(0, text)
                requireGreenBackgroundInChangedColumnInRow(0)
            }
        }
    }

    @Test
    fun shouldShowARemovedTextInRedInOriginalColumnOnly(): TestResult {
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
        return runReactTest(fc) { container ->
            with(container) {
                requireOriginalTextInRow(0, text)
                requireNoChangedTextInRow(0)
                requireRedBackgroundInOriginalColumnInRow(0)
            }
        }
    }

    @Test
    fun shouldShowAReplacedAndReplacementTextsInTheirRespectiveColumnsWithCorrespondingColours(): TestResult {
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
        return runReactTest(fc) { container ->
            with(container) {
                requireOriginalTextInRow(0, replaced)
                requireChangedTextInRow(0, replacement)
                requireRedBackgroundInOriginalColumnInRow(0)
                requireGreenBackgroundInChangedColumnInRow(0)
            }
        }
    }

    @Test
    fun shouldCallOnStartRuleWhenTheBuildIconIsClicked(): TestResult {
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
        return runReactTest(fc) { container ->
            with(container) {
                ruleStarted shouldBe false
                requireBuildIconForRow(0)
                clickBuildIconForRow(0)
                ruleStarted shouldBe true
            }
        }
    }

    @Test
    fun onStartRuleShouldIdentifyTheSelectedDiff(): TestResult {
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
        return runReactTest(fc) { container ->
            with(container) {
                moveMouseOverRow(2)
                waitForEvents()
                clickBuildIconForRow(2)
                //assertion is in onStartRule
            }
        }
    }
}