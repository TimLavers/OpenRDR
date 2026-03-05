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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SuggestionListRow(
    text: String,
    suggestions: List<String>,
    index: Int
) {
    val scrollState = rememberScrollState()
    val numberedText = suggestions.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics { contentDescription = "$BOT${index}" }
    ) {
        if (text.isNotEmpty()) {
            Text(
                text = text,
                color = Black,
                style = TextStyle(fontSize = 14.sp),
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 120.dp)
                .background(White, RoundedCornerShape(8.dp))
                .semantics { contentDescription = "$SUGGESTION_LIST$index" }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(8.dp)
            ) {
                Text(
                    text = numberedText,
                    color = Black,
                    style = TextStyle(fontSize = 13.sp)
                )
            }
            VerticalScrollbar(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState)
            )
        }
    }
}
