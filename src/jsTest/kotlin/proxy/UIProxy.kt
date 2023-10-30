package proxy

import io.kotest.matchers.shouldBe
import io.rippledown.casecontrol.POLL_PERIOD
import io.rippledown.constants.caseview.CASES
import io.rippledown.constants.caseview.NUMBER_OF_CASES_ID
import js.core.asList
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import web.dom.Element
import web.dom.NodeListOf
import web.html.HTMLElement
import kotlin.js.Date
import kotlin.js.JSON.stringify
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

fun HTMLElement.waitForElementToBeNull(selector: String, ms: Duration = 100.milliseconds) {
    debug("about to waitForElementToBeNull($selector)")
    var element: Element? = null
    /*setTimeout(ms) {
        element = querySelector(selector)
        if (element == null) {
            debug("element is null")
        } else {
            debug("element is not null")
        }
        element shouldBe null
    }*/
    debug("finished")
    debug("element is $element")
}

suspend fun waitForEvents(timeout: Long = 150) {
    withContext(Default) {
        delay(timeout) // Dispatchers.Default doesn't know about TestCoroutineScheduler
    }
}

suspend fun waitForNextPoll() =
    waitForEvents(POLL_PERIOD.plus(250.milliseconds).inWholeMilliseconds)
//longer than the delay for the 1st poll

fun HTMLElement.requireNumberOfCases(expected: Int) {
    numberOfCasesWaiting() shouldBe expected
}

fun HTMLElement.requireNumberOfCasesNotToBeShowing() {
    findAllById(NUMBER_OF_CASES_ID).length shouldBe 0
}

fun HTMLElement.numberOfCasesWaiting() = findById(NUMBER_OF_CASES_ID)
    .textContent!!
    .substringAfter("$CASES ")
    .toInt()


fun HTMLElement.printJSON() = debug("${stringify(outerHTML, null, space = 2)})}")

fun debug(msg: String) {
    println("\n\n${Date().toISOString()} $msg")
}

fun HTMLElement.requireElementContainingTextContent(text: String) {
    querySelectorAll("*").asList().any { element ->
        val content = element.textContent!!
        content.contains(text.toRegex())
    } shouldBe true
}

fun HTMLElement.findByTextContent(text: String): HTMLElement {
    return querySelectorAll("*").asList().first { element ->
        val content = element.textContent!!
        content == text
    } as HTMLElement
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




