package io.rippledown.kb

import Api
import io.kotest.matchers.shouldBe
import io.rippledown.constants.kb.KB_INFO_HEADING_ID
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import mocks.EngineConfig
import mocks.config
import mocks.mock
import react.FC
import react.dom.test.runReactTest
import web.html.HTML.button
import kotlin.test.Test

class KBInfoPaneTest {

    var testApi: Api = Api(mock(config { }))

    @Test
    fun checkInitialValues(): TestResult = runTest {
        val vfc = FC {
            KBInfoPane {
                api = testApi
                scope = MainScope()
            }
        }
        runReactTest(vfc) { container ->
            val headingById = container.querySelectorAll("[id='$KB_INFO_HEADING_ID']")[0]
            headingById.textContent shouldBe EngineConfig().returnKBInfo.name

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

    @Test
    fun controlsShouldBeHiddenWhenRuleBuilding(): TestResult = runTest {
        val vfc = FC {
            KBInfoPane {
                api = testApi
                scope = MainScope()
            }
        }
        runReactTest(vfc) { container ->
            val headingById = container.querySelectorAll("[id='$KB_INFO_HEADING_ID']")[0]
            headingById.textContent shouldBe EngineConfig().returnKBInfo.name

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