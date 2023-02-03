package mysticfall

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import js.core.get
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mui.lab.TreeItem
import mui.lab.TreeView
import react.*
import react.dom.test.checkContainer
import kotlin.test.Test

class TreeViewTest {

    val TreeViewWrapper = VFC {
        TreeView {
            TreeItem {
                nodeId = "id_1"
                label = "label_1".unsafeCast<ReactNode>()
            }
            TreeItem {
                nodeId = "id_2"
                label = "label_2".unsafeCast<ReactNode>()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun shouldAssignIdsAndLabelsUsingRunTest() = runTest {
        checkContainer(TreeViewWrapper) { container ->
            val elements = container.getElementsByClassName("MuiTreeItem-root")
            elements.length shouldBe 2
            elements[0].id shouldContain "id_1"
            elements[0].textContent shouldBe "label_1"
            elements[1].id shouldContain "id_2"
            elements[1].textContent shouldBe "label_2"
        }
    }
}

