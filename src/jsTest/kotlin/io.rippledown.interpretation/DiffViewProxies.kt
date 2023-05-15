package io.rippledown.interpretation

import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.interpretation.DIFF_VIEWER_BUILD_ICON
import io.rippledown.constants.interpretation.DIFF_VIEWER_CHANGED
import io.rippledown.constants.interpretation.DIFF_VIEWER_ORIGINAL
import io.rippledown.constants.interpretation.DIFF_VIEWER_ROW
import kotlinx.browser.window
import org.w3c.dom.Element
import proxy.findAllById
import proxy.findById
import react.dom.test.Simulate
import web.html.HTMLElement

fun HTMLElement.requireNumberOfRows(expected: Int) {
    findAllById(DIFF_VIEWER_ROW).length shouldBe expected
}

fun HTMLElement.moveMouseOverRow(row: Int) {
    val tableRow = findById("$DIFF_VIEWER_ROW$row")
    Simulate.mouseOver(tableRow)
}

fun HTMLElement.requireNoBuildIconForRow(row: Int) {
    try {
        val tableRow = findById("$DIFF_VIEWER_ROW$row")
        tableRow.findById("$DIFF_VIEWER_BUILD_ICON$row")
        fail("Expected no build icon for row $row")
    } catch (e: Error) {
        //expected
    }
}

fun HTMLElement.requireBuildIconForRow(row: Int) {
    findById("$DIFF_VIEWER_BUILD_ICON$row") shouldNotBe null
}

fun HTMLElement.clickBuildIconForRow(row: Int) {
    val icon = findById("$DIFF_VIEWER_BUILD_ICON$row")
    Simulate.click(icon)
}

fun HTMLElement.requireNoOriginalTextInRow(row: Int) {
    try {
        findById("$DIFF_VIEWER_ORIGINAL$row")
        fail("Expected no original text for row $row")
    } catch (e: Error) {
        //expected
    }
}

fun HTMLElement.requireNoChangedTextInRow(row: Int) {
    try {
        findById("$DIFF_VIEWER_CHANGED$row")
        fail("Expected no changed text for row $row")
    } catch (e: Error) {
        //expected
    }
}

fun HTMLElement.requireOriginalTextInRow(row: Int, text: String) {
    findById("$DIFF_VIEWER_ORIGINAL$row").textContent shouldBe text
}

fun HTMLElement.requireChangedTextInRow(row: Int, text: String) {
    findById("$DIFF_VIEWER_CHANGED$row").textContent shouldBe text
}

fun HTMLElement.requireRedBackgroundInOriginalColumnInRow(row: Int) {
    val original = findById("$DIFF_VIEWER_ORIGINAL$row")
    //TODO compare with a Color, not with a string
    window.getComputedStyle(original.unsafeCast<Element>()).backgroundColor shouldBe "rgb(240, 200, 200)"
}

fun HTMLElement.requireGreenBackgroundInChangedColumnInRow(row: Int) {
    val changed = findById("$DIFF_VIEWER_CHANGED$row")
    //TODO compare with a Color, not with a string
    window.getComputedStyle(changed.unsafeCast<Element>()).backgroundColor shouldBe "rgb(200, 240, 200)"
}

fun HTMLElement.requireNoColorBackgroundInChangedColumnInRow(row: Int) {
    val changed = findById("$DIFF_VIEWER_CHANGED$row")
    //TODO compare with a Color, not with a string
    window.getComputedStyle(changed.unsafeCast<Element>()).backgroundColor shouldBe "rgb(255, 255, 0)"

}

