package io.rippledown.chat

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun KbChoiceRow(
    names: List<String>,
    index: Int,
    onChosen: (String) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val encodedNames = names.joinToString("\n")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$BOT${index}" }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(White, RoundedCornerShape(8.dp))
                .semantics { contentDescription = "$KB_CHOICE_LIST$index:$encodedNames" }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                names.forEach { name ->
                    Box(
                        modifier = Modifier
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable { onChosen(name) }
                            .semantics { contentDescription = "$KB_CHOICE_ITEM$name" }
                    ) {
                        Text(
                            text = name,
                            color = Black,
                            style = TextStyle(fontSize = 13.sp)
                        )
                    }
                }
            }
            if (scrollState.maxValue > 0) {
                HorizontalScrollbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    adapter = rememberScrollbarAdapter(scrollState)
                )
            }
        }
    }
}
