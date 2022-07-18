import api.getWaitingCasesInfo
import csstype.FontFamily
import csstype.FontSize
import csstype.px
import csstype.rgb
import io.rippledown.model.CasesInfo
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import react.FC
import react.css.css
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.useEffectOnce
import react.useState

private val scope = MainScope()

val OpenRDRUI = FC<Props> {
    var waitingCasesInfo by useState(CasesInfo(0, ""))

    useEffectOnce {
        scope.launch {
            waitingCasesInfo = getWaitingCasesInfo()
        }
    }

    div {
        css {
            fontFamily = FontFamily.sansSerif
        }
        h1 {
            +"Open RippleDown"
            css {
//                backgroundColor = rgb(198, 0, 232)
                color = rgb(24, 24, 198)
            }
            id = "main_heading"
        }
        CaseQueue()
    }
}