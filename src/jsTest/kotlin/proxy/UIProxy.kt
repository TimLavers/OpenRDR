package proxy

import CASEVIEW_CASE_NAME_ID
import CASE_ID_PREFIX
import NUMBER_OF_CASES_WAITING_ID
import POLL_PERIOD
import io.kotest.matchers.shouldBe
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_CHANGES
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_AREA
import io.rippledown.interpretation.SEND_INTERPRETATION_BUTTON_ID
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import react.dom.test.Simulate
import react.dom.test.act
import web.dom.Element
import web.dom.NodeListOf
import web.events.EventInit
import web.html.HTMLElement
import kotlin.js.Date
import kotlin.js.JSON.stringify
import kotlin.time.Duration.Companion.milliseconds


suspend fun HTMLElement.clickSubmitButton() {
    val element = findById(SEND_INTERPRETATION_BUTTON_ID)
    act {
        Simulate.click(element)
    }
    debug("clicked submit button")
}


fun HTMLElement.requireCaseToBeSelected(caseName: String) {
    findById(CASEVIEW_CASE_NAME_ID).textContent shouldBe caseName
}

fun HTMLElement.requireNamesToBeShowingOnCaseList(vararg caseNames: String) {
    caseNames.forEach {
        findById("$CASE_ID_PREFIX$it").textContent shouldBe it
    }
}

suspend fun waitForEvents(timeout: Long = 150) {
    withContext(Default) {
        delay(timeout) // Dispatchers.Default doesn't know about TestCoroutineScheduler
    }
}

suspend fun waitForNextPoll() =
    waitForEvents(POLL_PERIOD.plus(250.milliseconds).inWholeMilliseconds)//longer than the delay for the 1st poll

fun HTMLElement.requireNumberOfCasesWaiting(expected: Int) {
    numberOfCasesWaiting() shouldBe expected
}

fun HTMLElement.numberOfCasesWaiting() = findById(NUMBER_OF_CASES_WAITING_ID)
    .textContent!!
    .substringAfter("Cases waiting: ")
    .toInt()

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

fun HTMLElement.printJSON() = debug("${stringify(outerHTML, null, space = 2)})}")

fun debug(msg: String) {
    println("\n\n${Date().toISOString()} $msg")
}

fun HTMLElement.findById(id: String): Element {
    val found = findAllById(id)
    if (found.length == 0) {
        throw Error("Element containing id \"$id\" not found")
    }
    if (found.length > 1) {
        throw Error("More than one element containing id \"$id\" found")
    }
    return found[0]
}

fun HTMLElement.findAllById(id: String): NodeListOf<Element> = querySelectorAll("[id*='$id']")

fun HTMLElement.selectCase(caseName: String) {
    val element = findById("$CASE_ID_PREFIX$caseName")
    Simulate.click(element)
}

fun HTMLElement.selectChangesTab() {
    val element = findById(INTERPRETATION_TAB_CHANGES)
    Simulate.click(element)
}