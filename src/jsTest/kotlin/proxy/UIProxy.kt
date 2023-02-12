package proxy

import CASELIST_ID
import CASEVIEW_CASE_NAME_ID
import CASE_ID_PREFIX
import CaseList
import INTERPRETATION_TEXT_AREA_ID
import NUMBER_OF_CASES_WAITING_ID
import POLL_PERIOD
import SEND_INTERPRETATION_BUTTON_ID
import io.kotest.assertions.timing.eventually
import io.kotest.matchers.shouldBe
import js.core.get
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mysticfall.TestInstance
import mysticfall.TestRenderer
import react.dom.events.ChangeEvent
import web.dom.Element
import web.dom.NodeListOf
import web.html.HTMLElement
import web.html.HTMLTextAreaElement
import kotlin.js.Date
import kotlin.js.JSON.stringify
import kotlin.time.Duration.Companion.milliseconds

fun TestRenderer.noItemWithIdIsShowing(id: String): Boolean {
    return itemWithId(id) == undefined
}

fun TestRenderer.findById(id: String): TestInstance<*> {
    val testInstance = itemWithId(id)

    return if (testInstance != undefined) {
        testInstance
    } else {
        throw Error("Instance with id \"$id\" not found")
    }
}

private fun TestRenderer.itemWithId(id: String): TestInstance<*> {
    val testInstance = root.findAll {
        it.props.asDynamic()["id"] == id
    }[0]
    return testInstance
}

suspend fun TestRenderer.waitForItemToHaveText( itemId: String, expectedText: String) {
    repeat(10) {
        if (findById(itemId).text() == expectedText) {
            return
        }
        waitForEvents()
    }
    throw Error("Text $expectedText not found for item with id $itemId. Current value is: ${findById(itemId).text()}")
}

fun TestInstance<*>.text() = props.asDynamic()["children"].unsafeCast<String>()

suspend fun TestInstance<*>.click() =
    coroutineScope {
        withContext(Default) {
            props.asDynamic().onClick() as Unit
        }
    }

suspend fun TestRenderer.clickSubmitButton() =
    findById(SEND_INTERPRETATION_BUTTON_ID).click()

suspend fun TestRenderer.selectCase(caseName: String) =
    findById("$CASE_ID_PREFIX$caseName").click()

fun TestRenderer.requireCaseToBeSelected(caseName: String) {
    val testInstance = findById(CASEVIEW_CASE_NAME_ID)
    val caseNameText = testInstance.props.asDynamic()["children"].unsafeCast<String>()
    caseNameText shouldBe caseName
}

fun TestRenderer.requireNamesToBeShowingOnCaseList(vararg caseNames: String) {
    val caseList = root.findByType(CaseList)
    caseList.props.caseIds.map { it.name } shouldBe caseNames.toList()
}

suspend fun waitForEvents(timeout: Long = 150) {
    withContext(Default) {
        delay(timeout) // Dispatchers.Default doesn't know about TestCoroutineScheduler
    }
}

suspend fun waitForNextPoll() =
    waitForEvents(POLL_PERIOD.plus(250.milliseconds).inWholeMilliseconds)//longer than the delay for the 1st poll

fun TestRenderer.requireNumberOfCasesWaiting(expected: Int) {
    numberOfCasesWaiting() shouldBe expected
}

fun TestRenderer.numberOfCasesWaiting() = findById(NUMBER_OF_CASES_WAITING_ID)
    .text()
    .substringAfter("Cases waiting: ")
    .toInt()

fun TestRenderer.requireCaseListNotToBeShowing() {
    root.findAllByType(CaseList) shouldBe emptyList<TestInstance<*>>()
}

fun TestRenderer.requireCaseListHeading(expected: String) {
    findById(CASELIST_ID).text() shouldBe expected
}

fun TestRenderer.requireInterpretation(expected: String) {
    val instance = interpretationArea()
    val value = instance.props.asDynamic()["value"].unsafeCast<String>()
    value shouldBe expected
}

fun TestRenderer.interpretationArea() = findById(INTERPRETATION_TEXT_AREA_ID)

fun TestRenderer.requireNoInterpretation() = requireInterpretation("")

/**
 * @see <a href="https://kotlinlang.org/docs/js-ir-migration.html#create-plain-js-objects-for-interoperability">Create plain JS objects for interoperability</a>
 */

fun TestRenderer.enterInterpretation(text: String) {
    val jsName = kotlin.js.json("value" to text)
    val jsEvent = kotlin.js.json("target" to jsName) as ChangeEvent<HTMLTextAreaElement>
    interpretationArea().props.asDynamic().onChange(jsEvent)
}

suspend fun waitFor(condition: () -> Boolean) = eventually { condition() shouldBe true }


fun TestRenderer.printJSON() = println("\n\n${stringify(toJSON(), null, space = 2)})}")

fun debug(msg: String) {
    println("\n\n${Date().toISOString()} $msg")
}

fun HTMLElement.findById(id: String): Element {
    val found = querySelectorAll("[id*='$id']")
    if (found.length == 0) {
        throw Error("Element containing id \"$id\" not found")
    }
    if (found.length > 1) {
        throw Error("More than one element containing id \"$id\" found")
    }
    return found[0]
}

fun HTMLElement.findAllById(id: String): NodeListOf<Element> = querySelectorAll("[id*='$id']")
