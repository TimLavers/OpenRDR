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
import proxy.*
import react.VFC
import react.dom.checkContainer
import react.dom.createRootFor
import react.dom.test.act
import kotlin.test.Test

class InterpretationTabsTest {

    @Test
    fun originalTabShouldBeSelectedByDefault() = runTest {
        val vfc = VFC {
            InterpretationTabs {
                interpretation = Interpretation()
                scope = this@runTest
            }
        }
        checkContainer(vfc) { container ->
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
            add(RuleSummary(conclusion = Conclusion(text)))
        }
        val vfc = VFC {
            InterpretationTabs {
                interpretation = originalInterp
                scope = this@runTest
            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                val originalPanel = findById(INTERPRETATION_TEXT_AREA)
                originalPanel.textContent shouldBe text
            }
        }

    }

    @Test
    fun shouldBeAbleToSelectTheChangesTab() = runTest {
        val vfc = VFC {
            InterpretationTabs {
                scope = this@runTest
                interpretation = Interpretation()
            }
        }
        val container = createRootFor(vfc)
        with(container) {
            act {
                selectChangesTab()
            }
            requireChangesLabel("Changes")
        }
    }

    @Test
    fun diffPanelShouldShowNoChangesForAnEmptyDiff() = runTest {
        val vfc = VFC {
            InterpretationTabs {
                scope = this@runTest
                interpretation = Interpretation()
            }
        }
        val container = createRootFor(vfc)
        with(container) {
            act {
                selectChangesTab()
            }
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

        val vfc = VFC {
            InterpretationTabs {
                scope = this@runTest
                api = Api(mock(config {}))
                interpretation = interpretationWithDiffs
            }
        }
        val container = createRootFor(vfc)
        with(container) {
            act {
                selectChangesTab()
            }
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
        val vfc = VFC {
            InterpretationTabs {
                scope = this@runTest
                api = Api(mock(config {}))
                interpretation = interpretationWithDiffs
            }
        }
        val container = createRootFor(vfc)
        with(container) {
            waitForEvents()
            findById("interpretation_changes_badge").textContent shouldBe "Changes3"
            requireBadgeCount(3) //Unchanged does not count
        }
    }

    @Test
    fun caseShouldBeRefreshedIfInterpretationIsEdited() = runTest {
        var refreshCaseCalled = false

        val vfc = VFC {
            InterpretationTabs {
                scope = this@runTest
                api = Api(mock(config {}))
                interpretation = Interpretation()
                refreshCase = {
                    refreshCaseCalled = true
                }

            }
        }
        val container = createRootFor(vfc)
        with(container) {
            refreshCaseCalled shouldBe false
            enterInterpretation("Go to Bondi now!")
            waitForDebounce()
            refreshCaseCalled shouldBe true
        }
    }

    @Test
    fun changesBadgeShouldNotShowIfNoChanges() = runTest {
        val vfc = VFC {
            InterpretationTabs {
                scope = this@runTest
                api = Api(mock(config {}))
                interpretation = Interpretation()
            }
        }
        val container = createRootFor(vfc)
        with(container) {
            requireNoBadge()
        }
    }
}

