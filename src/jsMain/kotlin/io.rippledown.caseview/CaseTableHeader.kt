package io.rippledown.caseview

import mui.material.TableCell
import mui.material.TableHead
import mui.material.TableRow
import mui.system.sx
import px4
import react.FC
import react.Props

external interface HeaderHandler: Props {
    var dates: List<Long>
}
val CaseTableHeader = FC<HeaderHandler> {
    TableHead {
        TableRow {
            TableCell {
                +"Attribute"
                sx {
                    padding = px4
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
                sx {
                    padding = px4
                }
                id = "case_table_header_reference_range"
            }
        }
    }
}
