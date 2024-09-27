package io.rippledown.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString

const val LEFT_INFO_MESSAGE_ID = "LEFT_INFO_MESSAGE_ID"
const val RIGHT_INFO_MESSAGE_ID = "RIGHT_INFO_MESSAGE_ID"

@Composable
fun InformationPanel(leftMessage: AnnotatedString, rightMessage: AnnotatedString) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = leftMessage,
            modifier = Modifier.semantics { contentDescription = LEFT_INFO_MESSAGE_ID })

        Text(text = rightMessage,
            modifier = Modifier.semantics { contentDescription = RIGHT_INFO_MESSAGE_ID })
    }
}