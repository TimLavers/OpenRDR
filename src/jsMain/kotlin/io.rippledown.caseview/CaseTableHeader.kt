package io.rippledown.caseview

import emotion.react.css
import mui.material.TableCell
import mui.material.TableHead
import mui.material.TableRow
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
    TableHead {
        TableRow {
            TableCell {
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
            TableCell {
                +"Reference Range"
                css {
                    padding = px8
                }
                id = "case_table_header_reference_range"
            }
        }
    }
}
