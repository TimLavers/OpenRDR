package diffviewer

import kotlinx.coroutines.test.runTest
import proxy.waitForEvents
import react.create
import react.dom.client.createRoot
import web.dom.Element
import web.dom.document
import kotlin.test.Ignore
import kotlin.test.Test

class DiffViewerTest {

    @Test
    @Ignore
    fun testDiffViewer() = runTest {
        println("document = ${document}")
        document.getElementById("root")?.let { container ->
            val ui = DifferenceViewer.create {
                oldValue = "Line 1\nLine 2a\nLine3"
                newValue = "Line 1\nLine 2b\nLine3"
            }
            createRoot(container.unsafeCast<Element>()).render(ui)
        }
        waitForEvents(10000)
    }

}