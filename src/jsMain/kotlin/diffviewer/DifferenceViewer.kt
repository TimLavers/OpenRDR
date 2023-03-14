package diffviewer

import react.FC
import react.create
import react.dom.client.createRoot
import react.dom.html.ReactHTML
import web.dom.Element
import web.dom.document

external interface DifferenceViewerProps : ReactDiffViewerProps

val DifferenceViewer = FC<DifferenceViewerProps> { handler ->
    ReactHTML.h1 {
        +"Difference Viewer"
    }
    DiffViewer.create {
        oldValue = handler.oldValue
        newValue = handler.newValue
        splitView = true
        hideLineNumbers = true
    }


}


fun main() {
    document.getElementById("root")?.let { container ->
        val ui = DifferenceViewer.create {
            this.oldValue = "Line 1\nLine 2a\nLine3"
            this.newValue = "Line 1\nLine 2b\nLine3"
        }
        createRoot(container.unsafeCast<Element>()).render(ui)
    }
}

