package io.rippledown.interpretation

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.interpretation.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import proxy.findById
import react.dom.test.Simulate
import react.dom.test.act
import web.events.EventInit
import web.html.HTMLElement

suspend fun HTMLElement.selectConclusionsTab() {
    val element = findById(INTERPRETATION_TAB_CONCLUSIONS)
    act {
        Simulate.click(element)
    }
}

fun HTMLElement.requireConclusionsLabel(expected: String) {
    findById(INTERPRETATION_TAB_CONCLUSIONS).textContent shouldBe expected
}

fun HTMLElement.selectChangesTab() {
    val element = findById(INTERPRETATION_TAB_CHANGES)
        Simulate.click(element)
}

fun HTMLElement.startToBuildRuleForRow(row: Int) {
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

fun HTMLElement.requireInterpretation(expected: String) {
    findById(INTERPRETATION_TEXT_AREA).textContent shouldBe expected
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

