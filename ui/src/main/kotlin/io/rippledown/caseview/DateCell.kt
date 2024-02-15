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
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun RowScope.DateCell(index: Int, date: Long, widthWeight: Float) {
    Text(
        text = formatDate(date),
        modifier = Modifier.weight(widthWeight)
            .semantics{
                contentDescription = dateCellContentDescription(index)
            },
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Start
    )
}

fun dateCellContentDescription(index: Int) = "$DATE_CELL_DESCRIPTION_PREFIX $index"

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
