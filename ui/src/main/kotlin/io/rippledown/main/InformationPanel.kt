package io.rippledown.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

const val INFO_MESSAGE_ID = "INFO_MESSAGE_ID"

@Composable
fun InformationPanel(message: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Text(text = message,
            modifier = Modifier.semantics { contentDescription = INFO_MESSAGE_ID })
    }
}