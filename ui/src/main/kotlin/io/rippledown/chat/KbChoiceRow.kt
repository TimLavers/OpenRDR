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
    val needsScroll = names.size > ROWS_BEFORE_SCROLL

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$BOT${index}" }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (needsScroll) Modifier.heightIn(max = MAX_PANEL_HEIGHT) else Modifier)
                .background(White, RoundedCornerShape(8.dp))
                .semantics { contentDescription = "$KB_CHOICE_LIST$index:$encodedNames" }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (needsScroll) Modifier.horizontalScroll(scrollState) else Modifier)
                    .padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
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
            if (needsScroll) {
                VerticalScrollbar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(scrollState)
                )
            }
        }
    }
}

private const val ROWS_BEFORE_SCROLL = 10
private val MAX_PANEL_HEIGHT = 180.dp
