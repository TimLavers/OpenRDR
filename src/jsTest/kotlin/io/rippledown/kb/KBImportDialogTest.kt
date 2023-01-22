package io.rippledown.kb

import Api
import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import mysticfall.ReactTestSupport
import mysticfall.TestRenderer
import proxy.*
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KBImportDialogTest : ReactTestSupport {

    @Test
    fun shouldShowInstructions() = runTest{
        lateinit var renderer: TestRenderer
        act {
            renderer = render {
                KBImportDialog {
                    scope = this@runTest
                    api = Api(mock(config {}))
                    reloadKB = {
                        println("reload kb in test")
                    }
                }
            }
        }
        val importButton = renderer.findById("import_from_zip")

        renderer.noItemWithIdIsShowing("import_kb_dialog_content")

        importButton.click()
//        renderer.findById("import_kb_dialog_content").text() shouldBe ""
//
//        renderer.waitForItemToHaveText(ID_KB_INFO_HEADING, "Glucose")
    }

    @Test
    fun importButton() = runTest {
        lateinit var renderer: TestRenderer
        act {
            renderer = render {
                KBInfoPane {
                    scope = this@runTest
                    api = Api(mock(config {}))
                }
            }
        }
        val importButton = renderer.findById("import_from_zip")
        importButton.text() shouldBe "Import"

        val dialog = renderer.findById("kb_import_dialog")
        dialog.props.asDynamic()["open"].unsafeCast<Boolean>() shouldBe false
//        with(renderer) {
//            renderer.findById("import_from_zip").click()
//            waitForEvents()
//        }
//        dialog.props.asDynamic()["open"].unsafeCast<Boolean>() shouldBe true
        importButton.props.asDynamic().onClick()
    }


}