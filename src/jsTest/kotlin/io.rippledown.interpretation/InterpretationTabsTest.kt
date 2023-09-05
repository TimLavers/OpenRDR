package io.rippledown.interpretation

import Api
import io.kotest.matchers.shouldBe
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_ORIGINAL
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_AREA
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.diff.*
import io.rippledown.model.rule.RuleSummary
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import proxy.findById
import proxy.waitForEvents
import react.FC
import react.dom.checkContainer
import react.dom.createRootFor
import kotlin.test.Test

class InterpretationTabsTest {

    @Test
    fun originalTabShouldBeSelectedByDefault() = runTest {
        val fc = FC {
            InterpretationTabs {
                interpretation = Interpretation()
                scope = this@runTest
            }
        }
        checkContainer(fc) { container ->
            with(container) {
                val originalTab = findById(INTERPRETATION_TAB_ORIGINAL)
                originalTab.textContent shouldBe "Interpretation"
            }
        }
    }

    @Test
    fun originalInterpretationShouldBeShowingByDefault() = runTest {
        val text = "Go to Bondi now!"
        val originalInterp = Interpretation().apply {
            add(RuleSummary(conclusion = Conclusion(1, text)))
        }
        val fc = FC {
            InterpretationTabs {
                interpretation = originalInterp
                scope = this@runTest
            }
        }
        checkContainer(fc) { container ->
            with(container) {
                val originalPanel = findById(INTERPRETATION_TEXT_AREA)
                originalPanel.textContent shouldBe text
            }
        }
    }

    @Test
    fun conclusionsTabShouldBeShowing() = runTest {
        val fc = FC {
            InterpretationTabs {
                scope = this@runTest
                interpretation = Interpretation()
            }
        }
        val container = createRootFor(fc)
        with(container) {
            requireConclusionsLabel("Conclusions")
        }
    }

    @Test
    fun conclusionsTabCanBeSelected() = runTest {
        val text = "Go to Bondi."
        val fc = FC {
            InterpretationTabs {
                interpretation = Interpretation().apply {
                    add(RuleSummary(conclusion = Conclusion(1, text)))
                }
            }
        }
        val container = createRootFor(fc)
        with(container) {
            selectConclusionsTab()
            requireTreeItems(text)
        }
    }

    @Test
    fun shouldBeAbleToSelectTheChangesTab() = runTest {
        val fc = FC {
            InterpretationTabs {
                scope = this@runTest
                interpretation = Interpretation()
            }
        }
        val container = createRootFor(fc)
        with(container) {
            selectChangesTab()
            requireChangesLabel("Changes")
        }
    }

    @Test
    fun diffPanelShouldShowNoChangesForAnEmptyDiff() = runTest {
        val fc = FC {
            InterpretationTabs {
                scope = this@runTest
                interpretation = Interpretation()
            }
        }
        val container = createRootFor(fc)
        with(container) {
            selectChangesTab()
            waitForEvents()
            requireNumberOfRows(0)
        }
    }

    @Test
    fun diffPanelShouldShowTheInterpretationDifferences() = runTest {
        val unchangedText = "Go to Bondi now!"
        val addedText = "Bring your flippers!"
        val removedText = "Sun is shining."
        val replacedText = "Surf's up!"
        val replacementText = "Surf's really up!"
        val diffListToReturn = DiffList(
            listOf(
                Unchanged(unchangedText),
                Addition(addedText),
                Removal(removedText),
                Replacement(replacedText, replacementText)
            )
        )
        val interpretationWithDiffs = Interpretation(diffList = diffListToReturn)

        val fc = FC {
            InterpretationTabs {
                scope = this@runTest
                api = Api(mock(config {}))
                interpretation = interpretationWithDiffs
            }
        }
        val container = createRootFor(fc)
        with(container) {
            selectChangesTab()
            requireNumberOfRows(4)
            requireOriginalTextInRow(0, unchangedText)
            requireChangedTextInRow(0, unchangedText)

            requireOriginalTextInRow(1, "")
            requireChangedTextInRow(1, addedText)

            requireOriginalTextInRow(2, removedText)
            requireChangedTextInRow(2, "")

            requireOriginalTextInRow(3, replacedText)
            requireChangedTextInRow(3, replacementText)
        }
    }

    @Test
    fun diffPanelShouldUpdateWhenTheInterpretationIsEdited() = runTest {
        val addedText = "Bring your flippers!"
        val diffListToReturn = DiffList(
            listOf(
                Addition(addedText),
            )
        )
        val interpretationWithDiffs = Interpretation(diffList = diffListToReturn)
        val config = config {
            returnInterpretationAfterSavingInterpretation = interpretationWithDiffs
        }

        val fc = FC {
            InterpretationTabs {
                scope = this@runTest
                api = Api(mock(config))
                interpretation = Interpretation()
            }
        }
        with(createRootFor(fc)) {
            requireInterpretation("")
            enterInterpretation(addedText)
            waitForDebounce()
            selectChangesTab()
            requireNumberOfRows(1)
            requireChangedTextInRow(0, addedText)
        }
    }


    @Test
    fun changesBadgeShouldIndicateTheNumberOfChanges() = runTest {
        val diffListToReturn = DiffList(
            listOf(
                Unchanged(),
                Addition(),
                Removal(),
                Replacement()
            )
        )
        val interpretationWithDiffs = Interpretation(diffList = diffListToReturn)
        val fc = FC {
            InterpretationTabs {
                scope = this@runTest
                api = Api(mock(config {}))
                interpretation = interpretationWithDiffs
            }
        }
        val container = createRootFor(fc)
        with(container) {
            waitForEvents()
            findById("interpretation_changes_badge").textContent shouldBe "Changes3"
            requireBadgeCount(3) //Unchanged does not count
        }
    }

    @Test
    fun changesBadgeShouldNotShowIfNoChanges() = runTest {
        val fc = FC {
            InterpretationTabs {
                scope = this@runTest
                api = Api(mock(config {}))
                interpretation = Interpretation()
            }
        }
        val container = createRootFor(fc)
        with(container) {
            requireNoBadge()
        }
    }

    @Test
    fun onStartRuleShouldBeCalledWhenTheBuildIconIsClicked() = runTest {
        val unchangedText = "Go to Bondi now!"
        val addedText = "Bring your flippers!"
        val removedText = "Sun is shining."
        val replacedText = "Surf's up!"
        val replacementText = "Surf's really up!"
        val diffListToReturn = DiffList(
            listOf(
                Unchanged(unchangedText),
                Addition(addedText),
                Removal(removedText),
                Replacement(replacedText, replacementText)
            )
        )
        val interpretationWithDiffs = Interpretation(diffList = diffListToReturn)
        var selectedDiff: Diff? = null

        val fc = FC {
            InterpretationTabs {
                scope = this@runTest
                api = Api(mock(config {}))
                interpretation = interpretationWithDiffs
                onStartRule = { diff ->
                    selectedDiff = diff
                }
            }
        }
        with(createRootFor(fc)) {
            selectChangesTab()
            requireNumberOfRows(4)
            moveMouseOverRow(2)
            clickBuildIconForRow(2)
            selectedDiff shouldBe diffListToReturn[2]
        }
    }

}

