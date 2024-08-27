package io.rippledown.interpretation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_FIELD

@Composable
fun InterpretationView(text: String) {
    OutlinedCard(
    ) {
            Text(
                text = text,
                modifier = Modifier.padding(10.dp)
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = INTERPRETATION_TEXT_FIELD
                    }

            )
    }
}