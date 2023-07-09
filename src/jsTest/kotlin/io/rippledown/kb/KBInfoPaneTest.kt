package io.rippledown.kb

import Api
import io.kotest.matchers.shouldBe
import io.rippledown.constants.kb.KB_INFO_HEADING_ID
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import react.FC
import react.dom.checkContainer
import web.html.HTML.button
import kotlin.test.Test

class KBInfoPaneTest {

    var testApi: Api = Api(mock(config { }))

    @Test
    fun initial(): TestResult = runTest {
        val vfc = FC {
            KBInfoPane {
                api = testApi
                scope = this@runTest
            }
        }
        checkContainer(vfc) { container ->
            val headingById = container.querySelectorAll("[id='$KB_INFO_HEADING_ID']")[0]
            headingById.textContent shouldBe "Glucose"

            val allButtons = container.getElementsByTagName(button)
            allButtons.length shouldBe 2
            val importButton = allButtons[0]
            importButton.textContent shouldBe "Import"
            importButton.disabled shouldBe false

            val exportButton = allButtons[1]
            exportButton.textContent shouldBe "Export"
            exportButton.disabled shouldBe false
        }
    }
}