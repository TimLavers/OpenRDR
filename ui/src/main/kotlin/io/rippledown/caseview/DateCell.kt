package io.rippledown.caseview

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import io.rippledown.constants.caseview.DATE_CELL_DESCRIPTION_PREFIX
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Composable
fun RowScope.DateCell(index: Int, date: Long, columnWidths: ColumnWidths) {
    // Take the full per-episode block width (value + value/range gap) so the
    // date label ("YYYY-MM-DD") doesn't wrap inside the narrower value
    // column. The gap to the right of the value is empty in the body, so
    // letting the date extend over it costs nothing visually.
    Text(
        text = formatDate(date),
        modifier = Modifier.weight(columnWidths.scrollableAreaWeight())
            .semantics{
                contentDescription = dateCellContentDescription(index)
            },
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Start
    )
}

fun dateCellContentDescription(index: Int) = "$DATE_CELL_DESCRIPTION_PREFIX $index"

@OptIn(ExperimentalTime::class)
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
    return "$datePart\n$timePart"
}
