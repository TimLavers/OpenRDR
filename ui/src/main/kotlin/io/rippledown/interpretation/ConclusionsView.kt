package io.rippledown.interpretation

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import io.rippledown.model.interpretationview.ViewableInterpretation


@Composable
fun ConclusionsView(interpretation: ViewableInterpretation) {
    Column {
        interpretation.conclusions().forEach { conclusion ->
            Text(conclusion.text)
        }
    }

}