import csstype.Cursor
import dom.html.HTMLTableCellElement
import emotion.react.css
import io.rippledown.model.Attribute
import kotlinx.coroutines.launch
import react.FC
import react.dom.events.DragEvent
import react.dom.html.ReactHTML

external interface AttributeCellHandler: Handler {
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
            println("Drag started, data tx is ${event.dataTransfer.getData("text")}")
        }
        onDragEnd = {
            println("Drag ended")
        }
        onDragOver = {
                event: DragEvent<HTMLTableCellElement> -> event.preventDefault()
        }
        onDrop = {
                event: DragEvent<HTMLTableCellElement> ->
            event.preventDefault()
            println("Dropped onto: ${it.attribute.name}")
            println("on drop data tx: ${event.dataTransfer.getData("text")}")
            val targetName = it.attribute.name
            val movedName = event.dataTransfer.getData("text")
            it.scope.launch {
                it.api.moveAttributeJustBelowOther(Attribute(movedName), Attribute(targetName))
                it.onCaseEdited()
            }
        }
    }
}