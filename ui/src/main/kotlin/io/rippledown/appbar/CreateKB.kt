package io.rippledown.appbar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

interface CreateKbHandler: TextInputHandler {
    fun handleDescription(description: String)
}

@Composable
fun CreateKB(handler: CreateKbHandler) {
    var descriptionValue by remember { mutableStateOf("") }

    Surface {
        Column {
            TextInputWithCancel(handler)
            OutlinedTextField(
                value = descriptionValue,
                enabled = true,
                onValueChange = { s ->
                    descriptionValue = s
                    handler.handleDescription(s)
                },
                label = { Text(text = "Description") },
                modifier = Modifier
//                    .fillMaxWidth()
                    .fillMaxSize()
                    .semantics {
//                    contentDescription = handler.inputFieldDescription()
                    }
//                .focusRequester(focusRequester)
            )
        }
    }
}