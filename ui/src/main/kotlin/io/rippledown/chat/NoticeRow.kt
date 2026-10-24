package io.rippledown.chat

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import io.rippledown.constants.chat.NOTICE_PREFIX

@Composable
fun NoticeRow(text: String, index: Int) {
    Text(
        text = text,
        style = MaterialTheme.typography.body2,
        fontStyle = FontStyle.Italic,
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .semantics { contentDescription = "$NOTICE_PREFIX$index:$text" }
    )
}
