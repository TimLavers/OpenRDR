package io.rippledown.interpretation

import Api
import io.kotest.matchers.shouldBe
import io.rippledown.model.Interpretation
import io.rippledown.model.diff.*
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import proxy.waitForEvents
import react.VFC
import react.dom.checkContainer
import react.dom.createRootFor
import react.dom.test.act
import kotlin.test.Test

class DiffViewerTest {

    @Test
    fun shouldNotShowAnyRowsIfNoChanges() = runTest {
        val vfc = VFC {
            DiffViewer {
                interpretation = Interpretation()
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
                interpretation = Interpretation(
                    diffList = DiffList(listOf(Unchanged(), Unchanged(), Unchanged()))
                )
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
                interpretation = Interpretation(
                    diffList = DiffList(listOf(Addition(), Removal(), Replacement()))
                )
            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                requireNumberOfRows(3)
            }
        }

    }

    @Test
    fun shouldShowABuildIconByDefaultForFirstUnchangedDiff() = runTest {
        val vfc = VFC {
            DiffViewer {
                interpretation = Interpretation(
                    diffList = DiffList(
                        listOf(
                            Unchanged(),
                            Unchanged(),
                            Unchanged(),
                            Addition(),
                            Unchanged()
                        )
                    )
                )
            }
        }
        val container = createRootFor(vfc)
        with(container) {
            requireBuildIconForRow(3)
        }
    }

    @Test
    fun shouldShowABuildIconWhenMouseIsOverChangedDiff() = runTest {
        val vfc = VFC {
            DiffViewer {
                interpretation = Interpretation(
                    diffList = DiffList(
                        listOf(
                            Addition(),
                            Unchanged(),
                            Removal(),
                            Unchanged()
                        )
                    )
                )
            }
        }
        val container = createRootFor(vfc)
        with(container) {
            moveMouseOverRow(2)
            requireBuildIconForRow(2)
        }
    }

    @Test
    fun shouldNotShowABuildIconWhenMouseIsOverUnchangedDiff() = runTest {
        val vfc = VFC {
            DiffViewer {
                interpretation = Interpretation(
                    diffList = DiffList(
                        listOf(
                            Addition(),
                            Unchanged(),
                            Removal(),
                            Unchanged()
                        )
                    )
                )
            }
        }
        val container = createRootFor(vfc)
        with(container) {
            moveMouseOverRow(1)
            requireNoBuildIconForRow(1)
        }
    }

    @Test
    fun shouldShowAnUnchangedTextInOriginalAndChangedColumns() = runTest {
        val text = "Go to Bondi now!"
        val vfc = VFC {
            DiffViewer {
                interpretation = Interpretation(
                    diffList = DiffList(
                        listOf(
                            Unchanged(text),
                        )
                    )
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
                interpretation = Interpretation(diffList = DiffList(diffs = listOf(Addition(text))))
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
                interpretation = Interpretation(
                    diffList = DiffList(
                        listOf(
                            Removal(text),
                        )
                    )
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
                interpretation = Interpretation(
                    diffList = DiffList(
                        listOf(
                            Replacement(replaced, replacement),
                        )
                    )
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

    @Test
    fun shouldCallOnStartRuleWhenTheBuildIconIsClicked() = runTest {
        var ruleStarted = false
        val vfc = VFC {
            DiffViewer {
                scope = this@runTest
                api = Api(mock(config {}))
                interpretation = Interpretation(
                    diffList = DiffList(
                        listOf(
                            Addition("Go to Bondi now!"),
                            Unchanged(),
                            Removal(),
                        )
                    )
                )
                onStartRule = {
                    ruleStarted = true
                }
            }
        }
        val container = createRootFor(vfc)
        with(container) {
            ruleStarted shouldBe false
            requireBuildIconForRow(0)
            clickBuildIconForRow(0)
            ruleStarted shouldBe true
        }
    }

    @Test
    fun onStartRuleShouldIdentifyTheSelectedDiff() = runTest {
        val interp = Interpretation(
            diffList = DiffList(
                listOf(
                    Addition("Go to Bondi now!"),
                    Unchanged("Enjoy the beach!"),
                    Removal("Go to Manly now!"),
                )
            )
        )
        val vfc = VFC {
            DiffViewer {
                scope = this@runTest
                interpretation = interp
                onStartRule = { interpretation ->
                    interpretation.diffList.selected shouldBe 2
                }
            }
        }
        val container = createRootFor(vfc)
        with(container) {
            moveMouseOverRow(2)
            waitForEvents()
            clickBuildIconForRow(2)
            //assertion is in onStartRule
        }
    }
}