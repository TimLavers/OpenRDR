import csstype.Cursor
import dom.html.HTMLTableCellElement
import dom.html.HTMLTableRowElement
import emotion.react.css
import io.rippledown.model.Attribute
import react.FC
import react.Props
import react.dom.events.DragEvent
import react.dom.html.ReactHTML

external interface AttributeCellHandler: Props {
    var attribute: Attribute
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
            println("Drag started")
            event.dataTransfer.setData("text", it.attribute.name)
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
            println("on drop: ${event.dataTransfer.getData("text")}")
        }
    }
}