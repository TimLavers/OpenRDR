package proxy

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.interpretation.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import react.dom.test.Simulate
import react.dom.test.act
import web.events.EventInit
import web.html.HTMLElement

fun HTMLElement.selectChangesTab() {
    val element = findById(INTERPRETATION_TAB_CHANGES)
    Simulate.click(element)
}

fun HTMLElement.requireChangesLabel(expected: String) {
    findById(INTERPRETATION_TAB_CHANGES).textContent shouldBe expected
}

fun HTMLElement.requireInterpretation(expected: String) {
    findById(INTERPRETATION_TEXT_AREA).textContent shouldBe expected
}

suspend fun HTMLElement.enterInterpretation(text: String) {
    val jsName = kotlin.js.json("value" to text)
    val jsEvent = kotlin.js.json("target" to jsName) as EventInit
    val element = findById(INTERPRETATION_TEXT_AREA)
    act {
        Simulate.change(element, jsEvent)
    }
}

suspend fun waitForDebounce() {
    withContext(Dispatchers.Default) {
        delay(DEBOUNCE_WAIT_PERIOD_MILLIS * 2)
    }
}

fun HTMLElement.requireBadgeCount(expected: Int) {
    querySelectorAll(".$BADGE_CLASS")[0].let {
        it.textContent!!.toInt() shouldBe expected
    }

}

fun HTMLElement.requireNoBadge() {
    querySelectorAll(".$BADGE_INVISIBLE_CLASS") shouldNotBe emptyList<HTMLElement>()
}

