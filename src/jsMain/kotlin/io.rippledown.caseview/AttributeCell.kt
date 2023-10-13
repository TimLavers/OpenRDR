package io.rippledown.caseview

import Handler
import debug
import io.rippledown.model.Attribute
import kotlinx.coroutines.launch
import mui.material.TableCell
import mui.system.sx
import px4
import react.FC
import react.dom.events.DragEvent
import web.cssom.Cursor
import web.html.HTMLTableCellElement

external interface AttributeCellHandler : Handler {
    var attribute: Attribute
    var onCaseEdited: () -> Unit
}

val AttributeCell = FC<AttributeCellHandler> { handler ->
    TableCell {
        +handler.attribute.name
        id = "attribute_name_cell_${handler.attribute.name}"
        sx {
            padding = px4
            cursor = Cursor.move
        }
        draggable = true

        onDragStart = { event ->
            debug("Drag started")
            event.dataTransfer.setData("text", "${handler.attribute.id}")
        }
        onDragOver = { event: DragEvent<HTMLTableCellElement> ->
            debug("Drag over")
            event.preventDefault()
        }
        onDrop = { event: DragEvent<HTMLTableCellElement> ->
            debug("Drop")
            event.preventDefault()
            val targetId = handler.attribute.id
            val movedId = event.dataTransfer.getData("text").toInt()
            handler.scope.launch {
                handler.api.moveAttributeJustBelowOther(movedId, targetId)
                handler.onCaseEdited()
            }
        }
    }
}