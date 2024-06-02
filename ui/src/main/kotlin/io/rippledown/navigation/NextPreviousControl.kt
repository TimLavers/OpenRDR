package io.rippledown.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.constants.navigation.INDEX_AND_TOTAL_ID
import io.rippledown.constants.navigation.NEXT_BUTTON
import io.rippledown.constants.navigation.OF
import io.rippledown.constants.navigation.PREVIOUS_BUTTON
import io.rippledown.decoration.ItalicGrey

interface NextPreviousControlHandler {
    fun next(): Unit
    fun previous(): Unit
}

@Composable
fun NextPreviousControl(currentIndex: Int, total: Int, handler: NextPreviousControlHandler) {
    Row(verticalAlignment = CenterVertically) {
        IconButton(
            onClick = { handler.previous() },
            enabled = currentIndex > 0
        ) {
            Icon(
                painter = painterResource("left-arrow_24.png"),
                contentDescription = PREVIOUS_BUTTON
            )
        }
        Text(
            //use 1-based index for display
            text = "${currentIndex + 1} $OF $total",
            style = ItalicGrey,
            modifier = Modifier
                .padding(5.dp)
                .semantics { contentDescription = INDEX_AND_TOTAL_ID }
        )
        IconButton(
            onClick = { handler.next() },
            enabled = currentIndex < total - 1
        ) {
            Icon(
                painter = painterResource("right-arrow_24.png"),
                contentDescription = NEXT_BUTTON
            )
        }
    }
}