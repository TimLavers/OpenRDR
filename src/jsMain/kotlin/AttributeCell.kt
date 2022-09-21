import io.rippledown.model.Attribute
import react.FC
import react.Props
import react.css.css
import react.dom.html.ReactHTML
import react.key

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