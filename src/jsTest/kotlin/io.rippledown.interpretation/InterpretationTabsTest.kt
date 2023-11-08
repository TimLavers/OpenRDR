package io.rippledown.interpretation

import io.kotest.matchers.shouldBe
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_ORIGINAL
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_AREA
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.diff.*
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.model.rule.RuleSummary
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.test.TestResult
import main.Api
import mocks.config
import mocks.mock
import proxy.findById
import proxy.waitForEvents
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

class InterpretationTabsTest {

    @Test
    fun originalTabShouldBeSelectedByDefault(): TestResult {
        val fc = FC {
            InterpretationTabs {
                interpretation = ViewableInterpretation()
                scope = MainScope()
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                val originalTab = findById(INTERPRETATION_TAB_ORIGINAL)
                originalTab.textContent shouldBe "Interpretation"
            }
        }
    }

    @Test
    fun originalInterpretationShouldBeShowingByDefault(): TestResult {
        val text = "Go to Bondi now!"
        val originalInterp = Interpretation().apply {
            add(RuleSummary(conclusion = Conclusion(1, text)))
        }
        val fc = FC {
            InterpretationTabs {
                interpretation = ViewableInterpretation(originalInterp)
                scope = MainScope()
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                val originalPanel = findById(INTERPRETATION_TEXT_AREA)
                originalPanel.textContent shouldBe text
            }
        }
    }

    @Test
    fun conclusionsTabShouldBeShowing(): TestResult {
        val fc = FC {
            InterpretationTabs {
                scope = MainScope()
                interpretation = ViewableInterpretation()
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                requireConclusionsLabel("Conclusions")
            }
        }
    }

    @Test
    fun conclusionsTabCanBeSelected(): TestResult {
        val text = "Go to Bondi."
        val interp = Interpretation().apply {
            add(RuleSummary(conclusion = Conclusion(1, text)))
        }
        val fc = FC {
            InterpretationTabs {
                interpretation = ViewableInterpretation(interp)
            }
        }

        return runReactTest(fc) { container ->
            with(container) {
                selectConclusionsTab()
                requireTreeItems(text)
            }
        }
    }

    @Test
    fun shouldBeAbleToSelectTheChangesTab(): TestResult {
        val fc = FC {
            InterpretationTabs {
                scope = MainScope()
                interpretation = ViewableInterpretation()
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                selectChangesTab()
                requireChangesLabel("Changes")
            }
        }
    }

    @Test
    fun diffPanelShouldShowNoChangesForAnEmptyDiff(): TestResult {
        val fc = FC {
            InterpretationTabs {
                scope = MainScope()
                interpretation = ViewableInterpretation()
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                selectChangesTab()
                waitForEvents()
                requireNumberOfRows(0)
            }
        }
    }

    @Test
    fun diffPanelShouldShowTheInterpretationDifferences(): TestResult {
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
        val interpretationWithDiffs = ViewableInterpretation().apply { diffList = diffListToReturn }

        val fc = FC {
            InterpretationTabs {
                scope = MainScope()
                api = Api(mock(config {}))
                interpretation = interpretationWithDiffs
            }
        }
        return runReactTest(fc) { container ->
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
    }

    @Test
    fun diffPanelShouldUpdateWhenTheInterpretationIsEdited(): TestResult {
        val addedText = "Bring your flippers!"
        val diffListToReturn = DiffList(
            listOf(
                Addition(addedText),
            )
        )
        val interpretationWithDiffs = ViewableInterpretation().apply { diffList = diffListToReturn }
        val config = config {
            returnInterpretationAfterSavingInterpretation = interpretationWithDiffs
        }

        val fc = FC {
            InterpretationTabs {
                scope = MainScope()
                api = Api(mock(config))
                interpretation = ViewableInterpretation()
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                requireInterpretation("")
                enterInterpretation(addedText)
                waitForDebounce()
                selectChangesTab()
                requireNumberOfRows(1)
                requireChangedTextInRow(0, addedText)
            }
        }
    }


    @Test
    fun changesBadgeShouldIndicateTheNumberOfChanges(): TestResult {
        val diffListToReturn = DiffList(
            listOf(
                Unchanged(),
                Addition(),
                Removal(),
                Replacement()
            )
        )
        val interpretationWithDiffs =
            ViewableInterpretation().apply { diffList = diffListToReturn }
        val fc = FC {
            InterpretationTabs {
                scope = MainScope()
                api = Api(mock(config {}))
                interpretation = interpretationWithDiffs
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                waitForEvents()
                findById("interpretation_changes_badge").textContent shouldBe "Changes3"
                requireBadgeCount(3) //Unchanged does not count
            }
        }
    }

    @Test
    fun changesBadgeShouldNotShowIfNoChanges(): TestResult {
        val fc = FC {
            InterpretationTabs {
                scope = MainScope()
                api = Api(mock(config {}))
                interpretation = ViewableInterpretation()
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                requireNoBadge()
            }
        }
    }

    @Test
    fun onStartRuleShouldBeCalledWhenTheBuildIconIsClicked(): TestResult {
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
        val interpretationWithDiffs =
            ViewableInterpretation().apply { diffList = diffListToReturn }
        var selectedDiff: Diff? = null

        val fc = FC {
            InterpretationTabs {
                scope = MainScope()
                api = Api(mock(config {}))
                interpretation = interpretationWithDiffs
                onStartRule = { diff ->
                    selectedDiff = diff
                }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                selectChangesTab()
                requireNumberOfRows(4)
                moveMouseOverRow(2)
                clickBuildIconForRow(2)
                selectedDiff shouldBe diffListToReturn[2]
            }
        }
    }
}

