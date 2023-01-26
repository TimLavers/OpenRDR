package io.rippledown.kb

import Api
import io.kotest.matchers.shouldBe
import io.ktor.util.*
import io.rippledown.model.*
import js.core.get
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestResult
import mocks.config
import mocks.mock
import react.VFC
import react.dom.test.runReactTest
import web.html.HTML.button
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KBInfoPaneTest  {

    var testApi: Api = Api(mock(config {  }))

    val KBInfoPaneWrapper = VFC {
        KBInfoPane {
            api = testApi
        }
    }

    @Test
    fun initial(): TestResult = runReactTest(KBInfoPaneWrapper) { container ->
        val headingById = container.querySelectorAll("[id='$ID_KB_INFO_HEADING']")[0]
        headingById.textContent shouldBe "Glucose"

        val allButtons = container.getElementsByTagName(button)
        allButtons.length shouldBe 1
        val importButton = allButtons[0]
        importButton.textContent shouldBe "Import"
        importButton.disabled shouldBe false
    }
}