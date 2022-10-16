import emotion.react.css
import io.rippledown.model.Attribute
import react.FC
import react.Props
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
        }
    }
}