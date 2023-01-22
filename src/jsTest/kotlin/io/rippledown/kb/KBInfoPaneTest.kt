package io.rippledown.kb

import Api
import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import js.core.get
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestResult
import react.VFC
import react.dom.test.runReactTest
import react.dom.test.simulateChange
import react.dom.test.simulateClick
import web.html.HTML.button
import web.html.HTML.div
import kotlin.test.Test
import kotlin.test.assertEquals

//val KBInfoPaneApp = VFC {
//    KB {
//        title = "Counter container"
//    }
//}


@OptIn(ExperimentalCoroutinesApi::class)
class KBInfoPaneTest  {

//    @Test
//    fun initial(): TestResult = runReactTest(KBInfoPane) { container ->
//    }
//
//    @Test
//    fun shouldShowKBName() = runTest{
//        lateinit var renderer: TestRenderer
//        act {
//            renderer = render {
//                KBInfoPane {
//                    scope = this@runTest
//                    api = Api(mock(config {}))
//                }
//            }
//        }
//        renderer.findById(ID_KB_INFO_HEADING).text() shouldBe ""
//
//        renderer.waitForItemToHaveText(ID_KB_INFO_HEADING, "Glucose")
//    }
//
//    @Test
//    fun importButton() = runTest {
//        lateinit var renderer: TestRenderer
//        act {
//            renderer = render {
//                KBInfoPane {
//                    scope = this@runTest
//                    api = Api(mock(config {}))
//                }
//            }
//        }
//        val importButton = renderer.findById("import_from_zip")
//        importButton.text() shouldBe "Import"
//
//        val dialog = renderer.findById("kb_import_dialog")
//        dialog.props.asDynamic()["open"].unsafeCast<Boolean>() shouldBe false
////        with(renderer) {
////            renderer.findById("import_from_zip").click()
////            waitForEvents()
////        }
////        dialog.props.asDynamic()["open"].unsafeCast<Boolean>() shouldBe true
////        importButton.props.asDynamic().onClick()
//    }
//

}