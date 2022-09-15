import csstype.FontFamily
import csstype.TextAlign
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
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
                color = rdBlue
                textAlign = TextAlign.center
            }
            id = "main_heading"
        }
        CaseQueue {
            getWaitingCasesInfo = {
                MainScope().async {
                    ApiClient().waitingCasesInfo()
                }.await()
            }
        }
    }
}