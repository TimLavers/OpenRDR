package io.rippledown.cornerstone

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.constants.cornerstone.EXEMPT_BUTTON
import io.rippledown.constants.navigation.INDEX_AND_TOTAL_ID
import io.rippledown.constants.navigation.NEXT_BUTTON
import io.rippledown.constants.navigation.OF
import io.rippledown.constants.navigation.PREVIOUS_BUTTON
import io.rippledown.decoration.ItalicGrey
import openrdr.ui.generated.resources.Res
import openrdr.ui.generated.resources.check_24
import openrdr.ui.generated.resources.left_arrow_24
import openrdr.ui.generated.resources.right_arrow_24
import org.jetbrains.compose.resources.painterResource

interface CornerstoneControlHandler {
    fun next(): Unit
    fun previous(): Unit
    fun exempt(): Unit
}

@Composable
fun CornerstoneControl(currentIndex: Int, total: Int, handler: CornerstoneControlHandler) {
    Row(
        verticalAlignment = CenterVertically,
        modifier = Modifier.height(40.dp)
    ) {
        IconButton(
            onClick = {
                handler.previous()
            },
            enabled = currentIndex > 0
        ) {
            Icon(
                painter = painterResource(Res.drawable.left_arrow_24),
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
            onClick = {
                handler.next()
            },
            enabled = currentIndex < total - 1
        ) {
            Icon(
                painter = painterResource(Res.drawable.right_arrow_24),
                contentDescription = NEXT_BUTTON
            )
        }
        IconButton(
            onClick = {
                handler.exempt()
            },
            enabled = total > 0
        ) {
            Icon(
                painter = painterResource(Res.drawable.check_24),
                contentDescription = EXEMPT_BUTTON
            )
        }
    }
}