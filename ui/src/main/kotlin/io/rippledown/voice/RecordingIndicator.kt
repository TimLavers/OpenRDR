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

/**
 * Visible cue that the voice service is currently recording. Gemini does
 * not stream partial transcriptions back, so without an explicit indicator
 * users have no feedback that they need to click the mic again to stop and
 * trigger transcription.
 *
 * Renders nothing while [VoiceRecognition.isListening] is `false`; renders
 * a small pill with a pulsing red dot and instructional text while `true`.
 */
@Composable
fun RecordingIndicator(voiceRecognitionService: VoiceRecognition) {
    val isListening by voiceRecognitionService.isListening.collectAsState()

    AnimatedVisibility(visible = isListening) {
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
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(pulse)
                    .background(Color(0xFFD32F2F), CircleShape)
            )
            Text(
                text = "  Recording\u2026 click the mic again to stop and transcribe",
                color = Color(0xFFB71C1C),
                style = TextStyle(fontSize = 12.sp)
            )
        }
    }
}
