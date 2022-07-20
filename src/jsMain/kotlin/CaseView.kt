import api.getWaitingCasesInfo
import csstype.*
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.RDRCase
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import react.*
import react.css.css
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.table
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.tr
import react.dom.html.ReactHTML.ul

private val scope = MainScope()

external interface CaseViewHandler : Props {
    var case: RDRCase
}

val CaseView = FC<CaseViewHandler> { props ->
    div {
        +props.case.name
        css {
            backgroundColor = rgb(192, 64, 128)
            float = Float.left
            width = Length("70%")
            padding = Length("12px")
        }
        table {
            tr {
                th {
                    +"Datum"
                    id = "case_table_header_datum"
                }
                th {
                    +"Value"
                    id = "case_table_header_value"
                }
            }
        }
    }
}
