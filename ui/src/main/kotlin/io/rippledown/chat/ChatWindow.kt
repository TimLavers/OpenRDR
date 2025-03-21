package io.rippledown.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

// Data class for messages
data class Message(val text: String, val isUser: Boolean)

// Chat window composable
@Composable
fun ChatWindow() {
    var inputText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<Message>() }
    val listState = rememberLazyListState()

    // Auto-scroll to the latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
        }
    }

    Surface(
        modifier = Modifier.width(300.dp).fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column {
            Text(
                "Chat with Assistant",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(8.dp)
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages) { message ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            modifier = Modifier.widthIn(max = 250.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(message.text, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Type your message") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    if (inputText.isNotBlank()) {
                        messages.add(Message(inputText, true))
                        val response = "Response to: $inputText" // Replace with your logic
                        messages.add(Message(response, false))
                        inputText = ""
                    }
                }) {
                    Text("Send")
                }
            }
        }
    }
}

// Main application composable
@Composable
fun App() {
    MaterialTheme {
        Row(modifier = Modifier.fillMaxSize()) {
            // Main content (left side)
            Column(modifier = Modifier.weight(1f)) {
                Text("Rule Builder", style = MaterialTheme.typography.titleLarge)
                // Add your main UI components here
            }
            // Chat window (right side)
            ChatWindow()
        }
    }
}

// Entry point
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Rule Builder") {
        App()
    }
}