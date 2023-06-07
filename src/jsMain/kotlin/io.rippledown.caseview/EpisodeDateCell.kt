package io.rippledown.caseview

import emotion.react.css
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mui.material.TableCell
import px8
import react.FC
import react.Props
import web.cssom.FontSize
import web.cssom.FontStyle
import web.cssom.FontWeight
import web.cssom.WhiteSpace

external interface EpisodeDateCellHandler: Props {
    var index: Int
    var date: Long
}
val EpisodeDateCell = FC<EpisodeDateCellHandler> {
    TableCell {
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
    val minutes = dateTime.time.minute
    val minutesRounded = if (dateTime.time.second >= 30) {
        minutes + 1
    } else {
        minutes
    }
    val minutesString = pad(minutesRounded)
    val timePart = "$hoursString:$minutesString"
    return "$datePart $timePart"
}