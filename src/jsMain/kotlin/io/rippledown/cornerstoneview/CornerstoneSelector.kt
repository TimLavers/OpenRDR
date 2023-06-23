package io.rippledown.cornerstoneview

import Handler
import debug
import mui.material.Pagination
import mui.material.PaginationColor
import mui.material.PaginationColor.Companion.primary
import mui.material.PaginationColor.Companion.secondary
import mui.material.Size
import mui.material.Size.small
import react.FC
import react.VFC

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
            debug("$event on change index $index")
            handler.onSelect(index.toInt())
        }
    }
}