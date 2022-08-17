import io.rippledown.model.Attribute
import react.FC
import react.Props
import react.css.css
import react.dom.html.ReactHTML

external interface HeaderHandler: Props {
    var dates: List<Long>
}
val CaseTableHeader = FC<HeaderHandler> {
    ReactHTML.thead {
        ReactHTML.tr {
            ReactHTML.th {
                +"Attribute"
                css {
                    padding = px8
                }
                id = "case_table_header_attribute"
            }
            ReactHTML.th {
                +"Value"
                css {
                    padding = px8
                }
                id = "case_table_header_value"
            }
            ReactHTML.th {
                +"Reference Range"
                css {
                    padding = px8
                }
                id = "case_table_header_reference_range"
            }
        }
    }

}