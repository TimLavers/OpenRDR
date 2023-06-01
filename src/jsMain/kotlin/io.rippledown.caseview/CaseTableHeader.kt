package io.rippledown.caseview

import emotion.react.css
import px8
import react.FC
import react.Props
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.thead
import react.dom.html.ReactHTML.tr

external interface HeaderHandler: Props {
    var dates: List<Long>
}
val CaseTableHeader = FC<HeaderHandler> {
    thead {
        tr {
            th {
                +"Attribute"
                css {
                    padding = px8
                }
                id = "case_table_header_attribute"
            }
            it.dates.forEachIndexed { i, d ->
                EpisodeDateCell {
                    index = i
                    date = d
                }
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
