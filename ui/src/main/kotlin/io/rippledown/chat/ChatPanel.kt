package io.rippledown.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement.End
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.rippledown.decoration.LIGHT_BLUE


data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

typealias OnMessageSent = (ChatMessage) -> Unit

const val USER = "USER_"
const val BOT = "BOT_"
const val CHAT_SEND = "CHAT_SEND"
const val CHAT_TEXT_FIELD = "CHAT_TEXT_FIELD"

@Composable
fun ChatPanel(messages: List<ChatMessage> = emptyList(), onMessageSent: OnMessageSent = {}) {
    var inputText by remember { mutableStateOf(TextFieldValue()) }
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
    ) {
        // Chat messages area
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .background(White, RoundedCornerShape(8.dp))
                    .semantics { contentDescription = CHAT_TEXT_FIELD },
                placeholder = { Text("Type your message...") },
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (inputText.text.isNotBlank()) {
                        onMessageSent(ChatMessage(inputText.text, true))
                        inputText = TextFieldValue("")

                        // Add bot message
                        val botResponse = getLLMResponse(inputText.text) // do this elsewhere
                        onMessageSent(ChatMessage(botResponse, false))
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = LIGHT_BLUE),
                modifier = Modifier.semantics { contentDescription = CHAT_SEND }
            ) {
                Text("Send", color = White)
            }
        }
    }
}

@Composable
fun UserRow(
    text: String,
    index: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = End
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            backgroundColor = LIGHT_BLUE,
            elevation = 2.dp,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = text,
                color = White,
                modifier = Modifier
                    .padding(12.dp)
                    .semantics {
                        contentDescription = "$USER${index}"
                    }
            )
        }
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
        Card(
            shape = RoundedCornerShape(12.dp),
            backgroundColor = White,
            elevation = 2.dp,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = text,
                color = Black,
                modifier = Modifier
                    .padding(12.dp)
                    .semantics {
                        contentDescription = "$BOT${index}"
                    }
            )
        }
    }
}

fun getLLMResponse(text: String): String {
    return "This is a response from the LLM to your message: $text"
}


fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Rule Builder Chat"
    ) {
        val messages = listOf(
            ChatMessage("Hello, how can I help you?", false),
            ChatMessage("I need help with my case.", true),
            ChatMessage("Sure! What do you need help with?", false)
        )
        MaterialTheme {
            ChatPanel(messages)
        }
    }
}

