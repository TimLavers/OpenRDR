@file:OptIn(ExperimentalFoundationApi::class)

package io.rippledown.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedSuggestionChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.rippledown.constants.chat.CHAT_BOT_PLACEHOLDER
import io.rippledown.decoration.LIGHT_BLUE

interface ChatMessage {
    val text: String
    val isUser: Boolean
}

data class UserMessage(
    override val text: String
) : ChatMessage {
    override val isUser: Boolean = true
}

data class BotMessage(
    override val text: String
) : ChatMessage {
    override val isUser: Boolean = false
}

typealias OnMessageSent = (UserMessage) -> Unit

const val USER = "USER_"
const val BOT = "BOT_"
const val CHAT_SEND = "CHAT_SEND"
const val CHAT_TEXT_FIELD = "CHAT_TEXT_FIELD"

@Composable
fun ChatPanel(
    caseId: Long = -1,
    sendIsEnabled: Boolean = true,
    messages: List<ChatMessage> = emptyList(),
    onMessageSent: OnMessageSent = {},
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf(TextFieldValue()) }
    val listState = rememberLazyListState()
    val textAreaFocusRequester = remember { FocusRequester() }

    // Request focus when the id of the case changes
    LaunchedEffect(caseId) {
        textAreaFocusRequester.requestFocus()
    }

    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .padding(start = 0.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)
            .widthIn(min = 300.dp)
            .background(Color(0xFFF5F5F5))
    ) {
        // Chat messages area
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 8.dp),
            state = listState,
            verticalArrangement = Arrangement.Bottom
        ) {
            itemsIndexed(messages) { index, message ->
                if (message.isUser) {
                    UserRow(message.text, index)
                } else {
                    BotRow(message.text, index)
                }
            }
        }

        // User input area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                enabled = sendIsEnabled,
                textStyle = TextStyle(fontSize = 14.sp),
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .background(White, RoundedCornerShape(8.dp))
                    .semantics { contentDescription = CHAT_TEXT_FIELD }
                    .onPreviewKeyEvent { event ->
                        if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                            inputText = sendUserMessage(inputText, onMessageSent, textAreaFocusRequester)
                            true // Consume the event to avoid newline insertion
                        } else {
                            false // Allow other events to be handled
                        }
                    }
                    .focusRequester(textAreaFocusRequester),
                placeholder = { Text(text = CHAT_BOT_PLACEHOLDER, style = TextStyle(fontSize = 14.sp)) },
                trailingIcon = {
                    TooltipArea(
                        tooltip = {
                            Surface(
                                modifier = Modifier
                                    .padding(4.dp)
                            ) {
                                Text(
                                    text = "Send",
                                    color = Black,
                                    style = TextStyle(fontSize = 12.sp)
                                )
                            }
                        },
                    ) {
                        FilledIconButton(
                            onClick = {
                                inputText = sendUserMessage(inputText, onMessageSent, textAreaFocusRequester)
                            },
                            enabled = inputText.text.isNotBlank(),
                            modifier = Modifier
                                .semantics { contentDescription = CHAT_SEND },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = LIGHT_BLUE
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Send",
                                tint = if (inputText.text.isNotBlank()) Blue else Color.Gray
                            )
                        }
                    }
                },
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }
    }
}

private fun sendUserMessage(
    inputText: TextFieldValue,
    onMessageSent: OnMessageSent,
    textAreaFocusRequester: FocusRequester
): TextFieldValue {
    if (inputText.text.isNotBlank()) {
        val messageText = inputText.text.trim()
        onMessageSent(UserMessage(messageText))
        textAreaFocusRequester.requestFocus() // Retain focus after button click
    }
    return TextFieldValue("")
}

@Composable
fun UserRow(
    text: String,
    index: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        ElevatedSuggestionChip(
            onClick = { },
            label = {
                Text(
                    text = text,
                    color = White,
                    style = TextStyle(fontSize = 14.sp)
                )
            },
            colors = SuggestionChipDefaults.elevatedSuggestionChipColors(
                containerColor = LIGHT_BLUE,
                labelColor = White
            ),
            modifier = Modifier
                .semantics {
                    contentDescription = "$USER${index}"
                }
        )
    }
}

@Composable
fun BotRow(
    text: String,
    index: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        ElevatedSuggestionChip(
            onClick = { },
            label = {
                Text(
                    text = text,
                    color = Black,
                    style = TextStyle(fontSize = 14.sp)
                )
            },
            colors = AssistChipDefaults.elevatedAssistChipColors(
                containerColor = White,
                labelColor = Black
            ),
            modifier = Modifier
                .semantics {
                    contentDescription = "$BOT${index}"
                }
        )
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Rule Builder Chat"
    ) {
        val messages = listOf(
            BotMessage("Hello, how can I help you?"),
            UserMessage("I need help with my case."),
            BotMessage("Sure! What do you need help with?")
        )
        MaterialTheme {
            ChatPanel(0, sendIsEnabled = true, messages)
        }
    }
}