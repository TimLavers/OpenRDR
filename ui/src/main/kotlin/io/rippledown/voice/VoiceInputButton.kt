package io.rippledown.voice

import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

const val CHAT_MIC_BUTTON = "CHAT_MIC_BUTTON"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceInputButton(
    voiceRecognitionService: VoiceRecognition,
    enabled: Boolean = true,
    onPartialResult: (String) -> Unit = {},
    onSegmentFinalized: (String) -> Unit = {}
) {
    val isListening by voiceRecognitionService.isListening.collectAsState()
    val partialResult by voiceRecognitionService.partialResult.collectAsState()
    val microphoneAvailable = remember { VoiceRecognitionService.isMicrophoneAvailable() }

    LaunchedEffect(partialResult) {
        onPartialResult(partialResult)
    }

    // The "Start recording" tooltip is helpful for discovery when the
    // button is idle. We deliberately do NOT show a "Stop recording"
    // tooltip while recording: the red MicOff icon plus the Recording
    // indicator pill above the input field already communicate the state
    // loudly, and another tooltip there is redundant.
    val micButton: @Composable () -> Unit = {
        androidx.compose.material3.IconButton(
            onClick = {
                if (isListening) {
                    voiceRecognitionService.stopListening()
                } else {
                    voiceRecognitionService.startListening(
                        scope = CoroutineScope(Dispatchers.Default)
                    ) { recognizedText ->
                        onSegmentFinalized(recognizedText)
                    }
                }
            },
            enabled = enabled,
            modifier = Modifier.size(32.dp).pointerHoverIcon(PointerIcon.Hand)
        ) {
            Icon(
                imageVector = if (isListening) Icons.Filled.MicOff else Icons.Filled.Mic,
                contentDescription = if (isListening) "Stop recording" else "Start recording",
                tint = if (!microphoneAvailable) Color.Gray else if (isListening) Color.Red else Blue,
                modifier = Modifier
                    .size(18.dp)
                    .semantics {
                        contentDescription = CHAT_MIC_BUTTON
                    }
            )
        }
    }

    if (isListening) {
        micButton()
    } else {
        // Material 3 TooltipBox uses BasicTooltipDefaults.GlobalMutatorMutex
        // by default, which coordinates dismissal across every TooltipBox
        // in the app: showing one cancels any other that's currently visible.
        // This is what makes the mic / send tooltips never get "stuck on"
        // when the user sweeps between them, unlike the old foundation
        // TooltipArea.
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
            tooltip = {
                PlainTooltip {
                    Text(
                        text = if (microphoneAvailable) "Start recording" else "No microphone available",
                        style = TextStyle(fontSize = 12.sp)
                    )
                }
            },
            state = rememberTooltipState()
        ) {
            micButton()
        }
    }
}
