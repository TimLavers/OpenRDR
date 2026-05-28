package io.rippledown.voice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val RECORDING_INDICATOR = "RECORDING_INDICATOR"
const val TRANSCRIBING_INDICATOR = "TRANSCRIBING_INDICATOR"

/**
 * Visible cue for the two non-idle states of the voice service.
 *
 * Gemini does not stream partial transcriptions back, so without explicit
 * indicators users have no feedback that (a) they need to click the mic
 * again to stop recording, and (b) the gap between stopping and the
 * transcript appearing is the network round-trip, not a hang.
 *
 *   - [VoiceRecognition.isListening]    -> red pulsing pill, "Recording\u2026"
 *   - [VoiceRecognition.isTranscribing] -> blue pill with three animated
 *                                          dots, "Transcribing"
 *   - otherwise                         -> renders nothing.
 */
@Composable
fun RecordingIndicator(voiceRecognitionService: VoiceRecognition) {
    val isListening by voiceRecognitionService.isListening.collectAsState()
    val isTranscribing by voiceRecognitionService.isTranscribing.collectAsState()

    AnimatedVisibility(visible = isListening) {
        RecordingPill()
    }
    AnimatedVisibility(visible = isTranscribing && !isListening) {
        TranscribingPill()
    }
}

@Composable
private fun RecordingPill() {
    val infinite = rememberInfiniteTransition()
    val pulse by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .semantics { contentDescription = RECORDING_INDICATOR },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(pulse)
                .background(Color(0xFFD32F2F), CircleShape)
        )
        Text(
            text = " Recording\u2026 click the mic again after speaking",
            color = Color(0xFFB71C1C),
            style = TextStyle(fontSize = 12.sp)
        )
    }
}

@Composable
private fun TranscribingPill() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .background(Color(0xFFE3F2FD), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .semantics { contentDescription = TRANSCRIBING_INDICATOR },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = " Transcribing",
            color = Color(0xFF0D47A1),
            style = TextStyle(fontSize = 12.sp)
        )
        AnimatedDots(color = Color(0xFF0D47A1))
    }
}

/**
 * Three dots that fade in sequence - same staggered-keyframe pattern as
 * the chat panel's TypingIndicator, inlined here so the dots inherit the
 * transcribing pill's blue tint.
 */
@Composable
private fun AnimatedDots(color: Color) {
    val infinite = rememberInfiniteTransition()
    val a1 by infinite.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.3f at 0; 1f at 200; 0.3f at 400; 0.3f at 1200
            },
            repeatMode = RepeatMode.Restart
        )
    )
    val a2 by infinite.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.3f at 0; 0.3f at 200; 1f at 400; 0.3f at 600; 0.3f at 1200
            },
            repeatMode = RepeatMode.Restart
        )
    )
    val a3 by infinite.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.3f at 0; 0.3f at 400; 1f at 600; 0.3f at 800; 0.3f at 1200
            },
            repeatMode = RepeatMode.Restart
        )
    )
    Row(
        modifier = Modifier.padding(start = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(4.dp).background(color.copy(alpha = a1), CircleShape))
        Box(Modifier.size(4.dp).background(color.copy(alpha = a2), CircleShape))
        Box(Modifier.size(4.dp).background(color.copy(alpha = a3), CircleShape))
    }
}
