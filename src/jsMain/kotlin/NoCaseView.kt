import csstype.Float
import csstype.FontStyle
import csstype.FontWeight
import csstype.pct
import emotion.react.css
import react.FC
import react.Props
import react.dom.html.ReactHTML.div


val NoCaseView = FC<Props> {
    div {
        + "No case selected"
        id = "no_case_view"
        css {
            float = Float.left
            width = 70.pct
            padding = px12
            paddingBottom = px4
            paddingLeft = px8
            color = rdBlue
            fontStyle = FontStyle.italic
            fontWeight = FontWeight.bold
        }
    }
}