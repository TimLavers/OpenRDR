import csstype.FontSize
import csstype.FontStyle
import csstype.FontWeight
import csstype.WhiteSpace
import react.FC
import react.Props
import react.css.css
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.th

import kotlinx.datetime.*
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
    fun pad(n: Int): String {
        return if (n < 10) "0$n" else "$n"
    }
    val instant = Instant.fromEpochMilliseconds(date)
    val dateTime = instant.toLocalDateTime(TimeZone.UTC)
    val datePart = dateTime.date.toString()
    val hoursString = pad(dateTime.time.hour)
    val minutesString = pad(dateTime.time.minute)
    val timePart = "$hoursString:$minutesString"
    return "$datePart $timePart"
}