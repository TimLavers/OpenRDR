package io.rippledown.kb

import Api
import io.kotest.matchers.shouldBe
import js.core.get
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestResult
import mocks.config
import mocks.mock
import react.VFC
import react.dom.test.runReactTest
import web.html.HTML.button
import web.html.HTML.dialog
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KBImportDialogTest {

    var testApi: Api = Api(mock(config {  }))

    val wrapper = VFC {
        KBImportDialog {
            api = testApi
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun initial(): TestResult = runReactTest(wrapper) { container ->
        val buttons = container.getElementsByTagName(button)
        buttons.length shouldBe 1

        val importButton = buttons[0]
        importButton.disabled shouldBe false
        importButton.textContent shouldBe "Import"

        val dialogs = container.getElementsByTagName(dialog)
        dialogs.length shouldBe 0 //It is not showing.
    }

//    @OptIn(ExperimentalCoroutinesApi::class)
//    @Test
//    fun showDialog(): TestResult = runReactTest(wrapper) { container ->
//        val buttons = container.getElementsByTagName(button)
//        val importButton = buttons[0]
//        importButton.simulateClick()
//
//        val instructionsElement = container.querySelectorAll("[id='import_kb_dialog_content']")[0]
//        println(instructionsElement)
//    }
}