package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.kb.*
import proxy.findAllById
import proxy.findById
import react.dom.test.act
import web.html.HTMLElement

fun HTMLElement.importKBButton() = findById(KB_IMPORT_BUTTON_ID)
fun HTMLElement.exportKBButton() = findById(KB_EXPORT_BUTTON_ID)
fun HTMLElement.confirmImportKBButton() = findById(CONFIRM_IMPORT_BUTTON_ID)
fun HTMLElement.cancelImportKBButton() = findById(CANCEL_IMPORT_BUTTON_ID)
fun HTMLElement.importKBDialogContent() = findById(KB_IMPORT_DIALOG_CONTENT)
fun HTMLElement.exportKBDialogContent() = findById(KB_EXPORT_DIALOG_CONTENT)

//fun HTMLElement.kbImportDialog() = screen.findById(KB_IMPORT_DIALOG) //todo
fun HTMLElement.kbImportDialog() = findById(KB_IMPORT_DIALOG)

fun HTMLElement.requireImportKBButtonToBeShowing() {
    importKBButton() shouldNotBe null
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

fun HTMLElement.requireImportDialogToBeShowing() {
    importKBDialogContent() shouldNotBe null
}

suspend fun HTMLElement.clickKBImport() {
    act { importKBButton().click() }
}