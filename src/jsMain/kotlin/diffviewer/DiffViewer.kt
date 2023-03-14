@file:JsModule("react-diff-viewer")
@file:JsNonModule

package diffviewer

import react.*

external interface ReactDiffViewerProps : Props {
    var oldValue: String
    var newValue: String
    var splitView: Boolean
    var hideLineNumbers: Boolean
}

@JsName("default")
external val ReactDiffViewer: FC<ReactDiffViewerProps>






