package io.rippledown.kb

import Api
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestResult
import mocks.config
import mocks.mock
import react.VFC
import react.dom.test.runReactTest
import web.html.HTML.button
import web.html.HTML.dialog
import kotlin.test.Test

class KBExportDialogTest {

    var testApi: Api = Api(mock(config {  }))

    val wrapper = VFC {
        KBExportDialog {
            api = testApi
        }
    }

        @Test
    fun initial(): TestResult = runReactTest(wrapper) { container ->
        val buttons = container.getElementsByTagName(button)
        buttons.length shouldBe 1

        val importButton = buttons[0]
        importButton.disabled shouldBe false
        importButton.textContent shouldBe "Export"

        val dialogs = container.getElementsByTagName(dialog)
        dialogs.length shouldBe 0 //It is not showing.
    }
}