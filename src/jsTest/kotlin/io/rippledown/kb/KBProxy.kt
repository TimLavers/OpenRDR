package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.beBlank
import io.rippledown.constants.kb.*
import kotlinx.browser.window
import proxy.findAllById
import proxy.findById
import react.dom.screen
import react.dom.test.act
import web.html.HTMLElement
import kotlin.js.json

fun HTMLElement.importKBButton() = findById(KB_IMPORT_BUTTON_ID)
fun HTMLElement.exportKBButton() = findById(KB_EXPORT_BUTTON_ID)
fun HTMLElement.createKBMenuItem() = findById(KB_CREATE_MENU_ITEM_ID)
fun HTMLElement.confirmImportKBButton() = findById(CONFIRM_IMPORT_BUTTON_ID)
fun HTMLElement.cancelImportKBButton() = findById(CANCEL_IMPORT_BUTTON_ID)
fun HTMLElement.importKBDialogContent() = findById(KB_IMPORT_DIALOG_CONTENT)
fun HTMLElement.exportKBDialogContent() = findById(KB_EXPORT_DIALOG_CONTENT)

fun HTMLElement.kbSelector() = findById(KB_SELECTOR_ID)

//fun HTMLElement.kbImportDialog() = screen.findById(KB_IMPORT_DIALOG) //todo
fun HTMLElement.kbImportDialog() = findById(KB_IMPORT_DIALOG)
fun HTMLElement.kbCreateDialog() = findById(KB_CREATE_DIALOG)

fun HTMLElement.requireKBName(name: String) {
    kbSelector().textContent shouldBe name
}

suspend fun HTMLElement.showKBCreateDialog() {
    act { createKBMenuItem().click() }
}

fun HTMLElement.requireImportKBButtonToBeShowing() {
    importKBButton() shouldNotBe null
}

fun HTMLElement.requireKBControlsToBeEnabled() {
    val element = kbSelector()
    window.getComputedStyle(element.unsafeCast<org.w3c.dom.Element>())
        .getPropertyValue("Mui-disabled") shouldBe beBlank()
}

fun HTMLElement.requireKBControlsToBeDisabled() {
    val element = kbSelector()
    window.getComputedStyle(element.unsafeCast<org.w3c.dom.Element>()).getPropertyValue("Mui-disabled") shouldNotBe null
}

fun HTMLElement.requireExportKBButtonToBeShowing() {
    exportKBButton() shouldNotBe null
}

fun HTMLElement.requireConfirmImportKBButtonToBeShowing() {
    kbImportDialog().confirmImportKBButton() shouldNotBe null
}

fun HTMLElement.requireImportDialogToNotBeShowing() {
    findAllById(KB_IMPORT_DIALOG_CONTENT).length shouldBe 0
}

fun HTMLElement.requireExportDialogToNotBeShowing() {
    findAllById(KB_EXPORT_DIALOG_CONTENT).length shouldBe 0
}

fun requireCreateKBDialogToNotBeShowing() {
    screen.queryAllByRole("presentation", json()).size shouldBe 0
}

fun requireCreateKBDialogToBeShowing() {
    kbCreateDialog() shouldNotBe null
}

fun kbCreateDialog(): HTMLElement {
    return screen.getAllByRole("presentation", json()).filter {
        it.id == KB_CREATE_DIALOG
    }[0] as HTMLElement
}

//TODO these are not working
suspend fun clickConfirmCreateKBButton() = act { kbCreateDialog().findById(CONFIRM_CREATE_BUTTON_ID) }
suspend fun clickCancelCreateKBButton() = act { kbCreateDialog().findById(CANCEL_CREATE_BUTTON_ID) }

suspend fun enterNewProjectName(name: String) {
    act {
        kbCreateDialog().textContent = name
    }
}

fun HTMLElement.requireImportDialogToBeShowing() {
    importKBDialogContent() shouldNotBe null
}

suspend fun HTMLElement.clickKBImport() {
    act { importKBButton().click() }
}

suspend fun HTMLElement.clickKBSelector() {
    act { kbSelector().click() }
}