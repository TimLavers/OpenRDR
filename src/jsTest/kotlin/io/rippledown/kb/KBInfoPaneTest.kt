package io.rippledown.kb

import Api
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import react.VFC
import react.dom.checkContainer
import web.html.HTML.button
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KBInfoPaneTest {

    var testApi: Api = Api(mock(config { }))

    val KBInfoPaneWrapper = VFC {
        KBInfoPane {
            api = testApi
        }
    }

    @Test
    fun initial(): TestResult = runTest {
        checkContainer(KBInfoPaneWrapper) { container ->
            val headingById = container.querySelectorAll("[id='$ID_KB_INFO_HEADING']")[0]
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