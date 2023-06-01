package io.rippledown.caseview

import Handler
import emotion.react.css
import io.rippledown.model.Attribute
import kotlinx.coroutines.launch
import px8
import react.FC
import react.dom.events.DragEvent
import react.dom.html.ReactHTML
import web.cssom.Cursor
import web.html.HTMLTableCellElement

external interface AttributeCellHandler : Handler {
    var attribute: Attribute
    var onCaseEdited: () -> Unit
}
val AttributeCell = FC<AttributeCellHandler> {
    ReactHTML.td {
        +it.attribute.name
        id = "attribute_name_cell_${it.attribute.name}"
        css {
            padding = px8
            cursor = Cursor.move
        }
        draggable = true
        onDragStart = { event ->
            event.dataTransfer.setData("text", it.attribute.name)
        }
        onDragOver = { event: DragEvent<HTMLTableCellElement> ->
            event.preventDefault()
        }
        onDrop = { event: DragEvent<HTMLTableCellElement> ->
            event.preventDefault()
            val targetName = it.attribute.name
            val movedName = event.dataTransfer.getData("text")
            it.scope.launch {
                it.api.moveAttributeJustBelowOther(Attribute(movedName), Attribute(targetName))
                it.onCaseEdited()
            }
        }
    }
}