@file:OptIn(ExperimentalFoundationApi::class)

package io.rippledown.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rippledown.decoration.LIGHT_BLUE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@Composable
fun VoiceInputButton(
    voiceRecognitionService: VoiceRecognitionService,
    enabled: Boolean = true,
    onTextUpdated: (String) -> Unit = {}
) {
    val isListening by voiceRecognitionService.isListening.collectAsState()
    val partialResult by voiceRecognitionService.partialResult.collectAsState()

    LaunchedEffect(partialResult) {
        if (partialResult.isNotBlank()) {
            onTextUpdated(partialResult)
        }
    }

    TooltipArea(
        tooltip = {
            Surface(modifier = Modifier.padding(4.dp)) {
                Text(
                    text = if (isListening) "Stop listening" else "Voice input",
                    color = Black,
                    style = TextStyle(fontSize = 12.sp)
                )
            }
        },
    ) {
        FilledIconButton(
            onClick = {
                if (isListening) {
                    voiceRecognitionService.stopListening()
                } else {
                    voiceRecognitionService.startListening(
                        scope = CoroutineScope(Dispatchers.Default)
                    ) { recognizedText ->
                        onTextUpdated(recognizedText)
                    }
                }
            },
            enabled = enabled,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isListening) Color.Red.copy(alpha = 0.15f) else LIGHT_BLUE
            )
        ) {
            Icon(
                imageVector = if (isListening) Icons.Filled.MicOff else Icons.Filled.Mic,
                contentDescription = if (isListening) "Stop listening" else "Voice input",
                tint = if (isListening) Color.Red else Blue,
                modifier = Modifier.semantics {
                    contentDescription = CHAT_MIC_BUTTON
                }
            )
        }
    }
}
