package io.rippledown.interpretation

import InterpretationTabs
import InterpretationTabsHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.rippledown.constants.main.TITLE
import io.rippledown.main.Api
import io.rippledown.main.Handler
import io.rippledown.main.handlerImpl
import io.rippledown.mocks.config
import io.rippledown.mocks.mock
import io.rippledown.model.CasesInfo
import io.rippledown.model.Interpretation
import io.rippledown.model.createCase
import io.rippledown.model.diff.*
import io.rippledown.model.interpretationview.ViewableInterpretation
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

class InterpretationTabsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `interpretation tab should be selected by default`() = runTest {
        with(composeTestRule) {
            setContent {
                InterpretationTabs(object : Handler by handlerImpl, InterpretationTabsHandler {
                    override var interpretation = ViewableInterpretation()
                    override var onStartRule: (selectedDiff: Diff) -> Unit = { }
                    override var isCornerstone = false
                })
            }
            requireInterpretation("")
        }
    }

    @Test
    fun `the latest text of the interpretation should be showing by default`() = runTest {
        val text = "Go to Bondi now!"
        val viewableInterpretation = ViewableInterpretation(Interpretation()).apply { verifiedText = text }
        with(composeTestRule) {
            setContent {
                InterpretationTabs(object : Handler by handlerImpl, InterpretationTabsHandler {
                    override var interpretation = viewableInterpretation
                    override var onStartRule: (selectedDiff: Diff) -> Unit = { }
                    override var isCornerstone = false

                })
            }
            requireInterpretation(text)
        }
    }

    @Test
    fun `changes to the interpretation should be saved`() = runTest {
        val text = "Go to Bondi now!"
        val originalInterpretation = ViewableInterpretation(Interpretation())
        val changedInterpretation = ViewableInterpretation(Interpretation()).apply { verifiedText = text }
        val config = config {
            expectedInterpretation = changedInterpretation
        }
        with(composeTestRule) {
            setContent {
                InterpretationTabs(object : Handler by handlerImpl, InterpretationTabsHandler {
                    override var interpretation = originalInterpretation
                    override var onStartRule: (selectedDiff: Diff) -> Unit = { }
                    override var isCornerstone = false
                    override var api = Api(mock(config))
                })
            }
            //Given
            requireInterpretation("")

            //When
            enterInterpretationAndWaitForUpdate(text)

            //Then
            requireInterpretation(text)
            //assertion that the interpretation was saved is in the config
        }
    }
    @Test
    fun `should not save interpretation if no changes have been made`() = runTest {
        val text = "Go to Bondi now!"
        val originalInterpretation = ViewableInterpretation(Interpretation())
        val changedInterpretation = ViewableInterpretation(Interpretation()).apply { verifiedText = text }
        val config = config {
            expectedInterpretation = changedInterpretation
        }
        with(composeTestRule) {
            setContent {
                InterpretationTabs(object : Handler by handlerImpl, InterpretationTabsHandler {
                    override var interpretation = originalInterpretation
                    override var onStartRule: (selectedDiff: Diff) -> Unit = { }
                    override var isCornerstone = false
                    override var api = Api(mock(config))
                })
            }
            //Given
            requireInterpretation("")

            //When
            enterInterpretationAndWaitForUpdate(text)

            //Then
            requireInterpretation(text)
            //assertion that the interpretation was saved is in the config
        }
    }
    /*

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

*/
}

fun main() {

    application {
        Window(
            onCloseRequest = ::exitApplication,
            icon = painterResource("water-wave-icon.png"),
            title = TITLE
        ) {
            val viewableInterpretation =
                ViewableInterpretation(Interpretation()).apply { verifiedText = "bondi or bust" }
            InterpretationTabs(object : Handler by handlerImpl, InterpretationTabsHandler {
                override var interpretation = viewableInterpretation
                override var onStartRule: (selectedDiff: Diff) -> Unit = { }
                override var isCornerstone = false

            })
        }
    }
}

