import csstype.*
import react.FC
import react.Props
import react.css.css
import react.dom.html.ReactHTML.div


val NoCaseView = FC<Props> {
    div {
        + "No case selected"
        id = "no_case_view"
        css {
            float = Float.left
            width = Length("70%")
            padding = px12
            paddingBottom = px4
            paddingLeft = px8
            color = rdBlue
            fontStyle = FontStyle.italic
            fontWeight = FontWeight.bold
        }
    }
}