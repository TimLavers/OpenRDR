@file:OptIn(ExperimentalMaterialApi::class)

package io.rippledown.interpretation

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuDefaults.TrailingIcon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.CenterEnd
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.rippledown.components.Scrollbar
import io.rippledown.constants.interpretation.COMMENT_SELECTOR_LABEL
import io.rippledown.constants.interpretation.DROP_DOWN_TEXT_FIELD

interface CommentSelectorHandler {
    fun onCommentChanged(comment: String)
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun CommentSelector(
    editable: Boolean,
    currentText: String,
    options: List<String>,
    label: String,
    prefix: String,
    handler: CommentSelectorHandler
) {
    var expanded by remember { mutableStateOf(false) }
    val filteredOptions = options.filteredBy(currentText)
    val scrollState = rememberScrollState()

    /**
     * @see <a href=https://stackoverflow.com/questions/67493387/detect-click-in-compose-textfield>StackOverflow: Detect click in compose TextField</a>
     */
    val source = remember { MutableInteractionSource() }
    if (source.collectIsPressedAsState().value) {
        println("EditableCommentSelector: clicked the dropdown from interaction source")
        expanded = true
    }

    println("EditableCommentSelector: current text is '$currentText' expanded is $expanded")
    Column(
        modifier = Modifier
            .semantics { contentDescription = "EXPOSED_DROPDOWN_MENU_BOX" },
    ) {
        OutlinedTextField(
//            readOnly = true,
            readOnly = !editable,
            value = currentText,
            interactionSource = source,
            onValueChange = {
                println("EditableCommentSelector: onValueChange: '$it'")
                handler.onCommentChanged(it)
            },
            label = {
                Text(
                    text = label,
                    style = TextStyle(fontStyle = FontStyle.Italic),
                    modifier = Modifier
                        .semantics { contentDescription = COMMENT_SELECTOR_LABEL }
                )
            },

            trailingIcon = { TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .onClick {
                    println("EditableCommentSelector: clicked the text field")
                }
                .fillMaxWidth()
                .semantics { contentDescription = DROP_DOWN_TEXT_FIELD }
                .clickable { expanded = true } //required for accessibility!
        )

        if (expanded) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.verticalScroll(scrollState)
                ) {
                    filteredOptions.forEach { option ->
                        Chip(
                            modifier = Modifier
                                .height(25.dp)
                                .semantics { contentDescription = "$prefix$option" },
                            colors = ChipDefaults.chipColors(
                                backgroundColor = Color.Transparent,
                            ),
                            onClick = {
                                handler.onCommentChanged(option.text)
                                expanded = false
                            },
                        ) {
                            Text(
                                text = option,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                Scrollbar(scrollState, modifier = Modifier.align(CenterEnd))
            }
        }
    }
}

/**
 * Returns the elements of [this] list that contain [text] as a subsequence, with the subsequence
 * underlined as an [AnnotatedString].
 *
 * @see <a href=https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/material3/material3/samples/src/main/java/androidx/compose/material3/samples/ExposedDropdownMenuSamples.kt>ExposedDropdownMenuSamples</a>
 */
private fun List<String>.filteredBy(text: CharSequence): List<AnnotatedString> {
    fun underlineSubsequence(needle: CharSequence, haystack: String): AnnotatedString? {
        return buildAnnotatedString {
            var i = 0
            for (char in needle) {
                val start = i
                haystack.indexOf(char, startIndex = i, ignoreCase = true).let {
                    if (it < 0) return null else i = it
                }
                append(haystack.substring(start, i))
                withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                    append(haystack[i])
                }
                i += 1
            }
            append(haystack.substring(i, haystack.length))
        }
    }
    return mapNotNull { option -> underlineSubsequence(text, option) }
}