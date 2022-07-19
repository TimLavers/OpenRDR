import csstype.FontFamily
import csstype.TextAlign
import csstype.rgb
import react.FC
import react.Props
import react.css.css
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1

val OpenRDRUI = FC<Props> {
    div {
        css {
            fontFamily = FontFamily.sansSerif
        }
        h1 {
            +"Open RippleDown"
            css {
                color = rgb(24, 24, 198)
                textAlign = TextAlign.center
            }
            id = "main_heading"
        }
        CaseQueue()
    }
}