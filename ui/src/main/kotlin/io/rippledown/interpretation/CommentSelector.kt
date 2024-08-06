package io.rippledown.interpretation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.material.ExposedDropdownMenuDefaults.TrailingIcon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import io.rippledown.constants.interpretation.COMMENT_SELECTOR_LABEL
import io.rippledown.constants.interpretation.DROP_DOWN_TEXT_FIELD

interface CommentSelectorHandler {
    fun onCommentSelected(comment: String)
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun CommentSelector(
    selectedComment: String,
    options: List<String>,
    label: String,
    prefix: String,
    handler: CommentSelectorHandler
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = it
        },
    ) {
        TextField(
            value = selectedComment,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = {
                Text(
                    text = label,
                    style = TextStyle(fontStyle = FontStyle.Italic),
                    modifier = Modifier.semantics { contentDescription = COMMENT_SELECTOR_LABEL }
                )
            },
            trailingIcon = { TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = DROP_DOWN_TEXT_FIELD }
                .clickable { expanded = true } //required for accessibility!
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    onClick = {
                        handler.onCommentSelected(option)
                        expanded = false
                    },
                    modifier = Modifier.semantics { contentDescription = "$prefix$option" },
                ) {
                    Text(
                        text = option,
                    )
                }
            }
        }
    }
}