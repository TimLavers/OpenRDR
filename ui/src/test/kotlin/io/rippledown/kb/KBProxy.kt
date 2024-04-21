package io.rippledown.kb


import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.kb.*

//TODO remove this
fun findById(id: String) {
    println("findById: $id")
}

fun importKBButton() = findById(KB_IMPORT_BUTTON_ID)
fun exportKBButton() = findById(KB_EXPORT_BUTTON_ID)
fun createKBMenuItem() = findById(KB_CREATE_MENU_ITEM_ID)
fun confirmImportKBButton() = findById(CONFIRM_IMPORT_BUTTON_ID)
fun cancelImportKBButton() = findById(CANCEL_IMPORT_BUTTON_ID)
fun importKBDialogContent() = findById(KB_IMPORT_DIALOG_CONTENT)
fun exportKBDialogContent() = findById(KB_EXPORT_DIALOG_CONTENT)

fun kbSelector() = findById(KB_SELECTOR_ID)

//fun kbImportDialog() = screen.findById(KB_IMPORT_DIALOG) //todo
fun kbImportDialog() = findById(KB_IMPORT_DIALOG)
fun kbCreateDialog() = findById(KB_CREATE_DIALOG)

fun requireKBName(name: String) {
//    kbSelector().textContent shouldBe name
}

suspend fun showKBCreateDialog() {
//    act { createKBMenuItem().click() }
}

fun requireImportKBButtonToBeShowing() {
    importKBButton() shouldNotBe null
}

fun requireKBControlsToBeEnabled() {
    val element = kbSelector()
//    window.getComputedStyle(element.unsafeCast<org.w3c.dom.Element>())
//        .getPropertyValue("Mui-disabled") shouldBe beBlank()
}

fun requireKBControlsToBeDisabled() {
    val element = kbSelector()
//    window.getComputedStyle(element.unsafeCast<org.w3c.dom.Element>()).getPropertyValue("Mui-disabled") shouldNotBe null
}

fun requireExportKBButtonToBeShowing() {
    exportKBButton() shouldNotBe null
}

fun requireConfirmImportKBButtonToBeShowing() {
//    kbImportDialog().confirmImportKBButton() shouldNotBe null
}

fun requireImportDialogToNotBeShowing() {
//    findAllById(KB_IMPORT_DIALOG_CONTENT).length shouldBe 0
}

fun requireExportDialogToNotBeShowing() {
//    findAllById(KB_EXPORT_DIALOG_CONTENT).length shouldBe 0
}

fun requireCreateKBDialogToNotBeShowing() {
//    screen.queryAllByRole("presentation", json()).size shouldBe 0
}

fun requireCreateKBDialogToBeShowing() {
    kbCreateDialog() shouldNotBe null
}



//TODO these are not working
//suspend fun clickConfirmCreateKBButton() = act { kbCreateDialog().findById(CONFIRM_CREATE_BUTTON_ID) }
//suspend fun clickCancelCreateKBButton() = act { kbCreateDialog().findById(CANCEL_CREATE_BUTTON_ID) }

suspend fun enterNewProjectName(name: String) {
//        kbCreateDialog().textContent = name
}

fun requireImportDialogToBeShowing() {
    importKBDialogContent() shouldNotBe null
}

suspend fun clickKBImport() {
//    act { importKBButton().click() }
}

suspend fun clickKBSelector() {
//    act { kbSelector().click() }
}