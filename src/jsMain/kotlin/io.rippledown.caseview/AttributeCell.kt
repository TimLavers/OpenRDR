package io.rippledown.caseview

import Handler
import emotion.react.css
import io.rippledown.model.Attribute
import kotlinx.coroutines.launch
import mui.material.TableCell
import mui.system.sx
import px4
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
    TableCell {
        +it.attribute.name
        id = "attribute_name_cell_${it.attribute.name}"
        sx {
            padding = px4
            cursor = Cursor.move
        }
        draggable = true
        onDragStart = { event ->
            event.dataTransfer.setData("text", "${it.attribute.id}")
        }
        onDragOver = { event: DragEvent<HTMLTableCellElement> ->
            event.preventDefault()
        }
        onDrop = { event: DragEvent<HTMLTableCellElement> ->
            event.preventDefault()
            val targetId = it.attribute.id
            val movedId = event.dataTransfer.getData("text").toInt()
            it.scope.launch {
                it.api.moveAttributeJustBelowOther(movedId, targetId)
                it.onCaseEdited()
            }
        }
    }
}