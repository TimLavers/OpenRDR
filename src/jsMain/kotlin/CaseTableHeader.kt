import csstype.FontSize
import csstype.FontStyle
import csstype.FontWeight
import csstype.WhiteSpace
import io.rippledown.model.Attribute
import react.FC
import react.Props
import react.css.css
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.th

import kotlinx.datetime.*
import react.ReactDsl
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
external interface EpisodeDateCellHandler: Props {
    var index: Int
    var date: Long
}
val EpisodeDateCell = FC<EpisodeDateCellHandler> {
    th {
        +formatDate(it.date)
        css {
            padding = px8
            fontWeight = FontWeight.normal
            fontStyle = FontStyle.italic
            fontSize = FontSize.small
            whiteSpace = WhiteSpace.breakSpaces
        }
        id = "episode_date_cell_${it.index}"
    }
}
fun formatDate(date: Long): String {
    val instant = Instant.fromEpochMilliseconds(date)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val datePart = dateTime.date.toString()
    val timePart = "${dateTime.time.hour}:${dateTime.time.minute}"
    return "$datePart $timePart"
}