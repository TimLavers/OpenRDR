package io.rippledown.interpretation

import Api
import io.kotest.matchers.shouldBe
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_CHANGES
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_ORIGINAL
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.diff.DiffList
import io.rippledown.model.diff.Unchanged
import io.rippledown.model.rule.RuleSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import proxy.findById
import proxy.selectChangesTab
import proxy.waitForEvents
import react.VFC
import react.dom.checkContainer
import react.dom.createRootFor
import react.dom.test.act
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InterpretationTabsTest {

    @Test
    fun originalTabShouldBeSelectedByDefault() = runTest {
        val vfc = VFC {
            InterpretationTabs {
                interpretation = Interpretation()
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
            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                val originalPanel = findById(INTERPRETATION_TEXT_AREA_ID)
                originalPanel.textContent shouldBe text
            }
        }

    }

    @Test
    fun shouldBeAbleToSelectTheDiffTab() = runTest {
        val vfc = VFC {
            InterpretationTabs {
                scope = this@runTest
                api = Api(mock(config {}))
                interpretation = Interpretation()
            }
        }
        val container = createRootFor(vfc)
        with(container) {
            act {
                selectChangesTab()
            }
            val changesTab = findById(INTERPRETATION_TAB_CHANGES)
            changesTab.textContent shouldBe "Changes"
        }
    }

    @Test
    fun DiffPanelShouldShowNoChangesByDefault() = runTest {
        val vfc = VFC {
            InterpretationTabs {
                scope = this@runTest
                api = Api(mock(config {}))
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
    fun shouldRefreshTheDiffsWhenTheChangesTabIsSelected() = runTest {
        val text = "Go to Bondi now!"
        val config = config {
            returnDiffList = DiffList(listOf(Unchanged(text)))
        }
        val vfc = VFC {
            InterpretationTabs {
                scope = this@runTest
                api = Api(mock(config))
                interpretation = Interpretation()
            }
        }
        val container = createRootFor(vfc)
        with(container) {
            selectChangesTab()
            waitForEvents()
            requireOriginalTextInRow(0, text)
            requireChangedTextInRow(0, text)
        }
    }
}

