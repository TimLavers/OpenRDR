package io.rippledown.interpretation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_FIELD

@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.requireInterpretation(text: String) {
    waitUntilExactlyOneExists(hasText(text))
}

@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.enterInterpretationAndWaitForUpdate(enteredText: String) {
    enterInterpretation(enteredText)
    waitUntilExactlyOneExists(hasText(enteredText))
}
fun ComposeTestRule.enterInterpretation(enteredText: String) {
    onNodeWithContentDescription(INTERPRETATION_TEXT_FIELD).performTextInput(enteredText)
}
fun ComposeTestRule.replaceInterpretationBy(enteredText: String) {
    onNodeWithContentDescription(INTERPRETATION_TEXT_FIELD).performTextReplacement(enteredText)
}
fun ComposeTestRule.requireInterpretationTabToBeDisplayed() {
    onNodeWithContentDescription(INTERPRETATION_TEXT_FIELD).assertIsDisplayed()
}

/*

suspend fun HTMLElement.selectConclusionsTab() {
    val element = findById(INTERPRETATION_TAB_CONCLUSIONS)
    act {
        Simulate.click(element)
    }
}

fun HTMLElement.requireConclusionsLabel(expected: String) {
    findById(INTERPRETATION_TAB_CONCLUSIONS).textContent shouldBe expected
}

suspend fun HTMLElement.selectChangesTab() {
    val element = findById(INTERPRETATION_TAB_CHANGES)
    act { Simulate.click(element) }
}

suspend fun HTMLElement.startToBuildRuleForRow(row: Int) {
    selectChangesTab()
    moveMouseOverRow(0)
    clickBuildIconForRow(row)
}

suspend fun HTMLElement.selectInterpretationTab() {
    val element = findById(INTERPRETATION_TAB_ORIGINAL)
    act {
        Simulate.click(element)
    }
}

fun HTMLElement.requireChangesLabel(expected: String) {
    findById(INTERPRETATION_TAB_CHANGES).textContent shouldBe expected
}



suspend fun HTMLElement.enterInterpretation(text: String) {
    val jsName = kotlin.js.json("value" to text)
    val jsEvent = kotlin.js.json("target" to jsName).unsafeCast<EventInit>()
    val element = findById(INTERPRETATION_TEXT_AREA)
    act {
        Simulate.change(element, jsEvent)
    }
}

//TODO should be wrapped in act{}
suspend fun waitForDebounce() {
    withContext(Dispatchers.Default) {
        delay(DEBOUNCE_WAIT_PERIOD_MILLIS * 2)
    }
}

fun HTMLElement.requireBadgeCount(expected: Int) {
    querySelectorAll(".$BADGE_CLASS")[0].let {
        val text = it.textContent!!
        withClue("Expected badge count to be $expected but was blank") {
            text shouldNotBe ""
        }
        text.toInt() shouldBe expected
    }
}

fun HTMLElement.requireNoBadge() {
    querySelectorAll(".$BADGE_INVISIBLE_CLASS") shouldNotBe emptyList<HTMLElement>()
}


 */