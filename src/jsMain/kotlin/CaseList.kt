import api.getWaitingCasesInfo
import csstype.*
import io.rippledown.model.CasesInfo
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import react.*
import react.css.css
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.ul

private val scope = MainScope()

val CaseList = FC<Props> {
    div {
        css {
            after {
                display = Display.table
                clear = Clear.both
            }
        }
        div {
            +"Cases "
            css {
                className = "left_column"
                backgroundColor = rgb(128, 128, 128)
                float = Float.left
                width = Length("10%")
                padding = Length("12px")
            }
            ul {
                li {
                    +"Case1"
                }
                li {
                    +"Case2"
                }
            }
        }
        div {
            +"Whatever"
            css {
                backgroundColor = rgb(192, 64, 128)
                float = Float.left
                width = Length("70%")
                padding = Length("12px")
            }
        }
    }
}