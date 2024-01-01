package io.rippledown.appbar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import io.rippledown.constants.main.*

interface CreateKBHandler {
    fun create(name: String)
    fun cancel()
}

@Composable
fun CreateKB(handler: CreateKBHandler) {
    var kbName by remember { mutableStateOf("") }
    fun isNameOK() = kbName.isNotBlank()

    MaterialTheme {
        Column {
            Text(
                text = CREATE_KB_NAME,
                color = androidx.compose.ui.graphics.Color.Companion.Black,
                textAlign = TextAlign.Start
            )
            TextField(
                value = kbName,
                enabled = true,
                onValueChange = { s ->
                    kbName = s
                    println("Text is: $s")
                },
                modifier = Modifier.testTag(CREATE_KB_NAME_FIELD_ID)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
            ) {
                Button(
                    onClick = {
                        handler.create(kbName)
                    },
                    enabled = isNameOK(),
                    modifier = Modifier.testTag(CREATE_KB_OK_BUTTON_ID)
                ) {
                    Text(OK)
                }
                Button(
                    onClick = {
                        handler.cancel()
                    },
                    modifier = Modifier.testTag(CREATE_KB_CANCEL_BUTTON_ID)
                ) {
                    Text(CANCEL)
                }
            }
        }
    }
}
