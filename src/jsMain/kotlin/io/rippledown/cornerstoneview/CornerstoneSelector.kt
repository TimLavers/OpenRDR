package io.rippledown.cornerstoneview

import Handler
import mui.material.Pagination
import mui.material.PaginationColor.Companion.secondary
import mui.material.Size.small
import react.FC

external interface CornerstoneSelectorHandler : Handler {
    var onSelect: (index: Int) -> Unit
    var total : Int
}

val CornerstoneSelector = FC<CornerstoneSelectorHandler> { handler ->
    Pagination {
        size = small
        count = handler.total
        siblingCount = 1
        boundaryCount = 1
        color = secondary
        onChange = { event, index ->
            handler.onSelect(index.toInt() - 1)//switch to 0-based index
        }
    }
}