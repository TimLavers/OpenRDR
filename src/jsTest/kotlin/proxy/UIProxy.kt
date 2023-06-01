package proxy

import io.rippledown.caseview.CASEVIEW_CASE_NAME_ID
import io.rippledown.caselist.CASE_ID_PREFIX
import io.rippledown.caselist.POLL_PERIOD
import io.kotest.matchers.shouldBe
import io.rippledown.constants.caseview.CASES
import io.rippledown.constants.caseview.NUMBER_OF_CASES_ID
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import react.dom.test.Simulate
import web.dom.Element
import web.dom.NodeListOf
import web.html.HTMLElement
import kotlin.js.Date
import kotlin.js.JSON.stringify
import kotlin.time.Duration.Companion.milliseconds

fun HTMLElement.requireCaseToBeSelected(caseName: String) {
    findById(CASEVIEW_CASE_NAME_ID).textContent shouldBe caseName
}



suspend fun waitForEvents(timeout: Long = 150) {
    withContext(Default) {
        delay(timeout) // Dispatchers.Default doesn't know about TestCoroutineScheduler
    }
}

suspend fun waitForNextPoll() =
    waitForEvents(POLL_PERIOD.plus(250.milliseconds).inWholeMilliseconds)//longer than the delay for the 1st poll

fun HTMLElement.requireNumberOfCases(expected: Int) {
    numberOfCasesWaiting() shouldBe expected
}

fun HTMLElement.numberOfCasesWaiting() = findById(NUMBER_OF_CASES_ID)
    .textContent!!
    .substringAfter("$CASES ")
    .toInt()


fun HTMLElement.printJSON() = debug("${stringify(outerHTML, null, space = 2)})}")

fun debug(msg: String) {
    println("\n\n${Date().toISOString()} $msg")
}

fun HTMLElement.findById(id: String): HTMLElement {
    val found = findAllById(id)
    if (found.length == 0) {
        throw Error("Element containing id \"$id\" not found")
    }
    if (found.length > 1) {
        throw Error("More than one element containing id \"$id\" found")
    }
    return found[0] as HTMLElement
}

fun HTMLElement.findAllById(id: String): NodeListOf<Element> = querySelectorAll("[id*='$id']")

fun HTMLElement.selectCase(caseName: String) {
    val element = findById("$CASE_ID_PREFIX$caseName")
    Simulate.click(element)
}

