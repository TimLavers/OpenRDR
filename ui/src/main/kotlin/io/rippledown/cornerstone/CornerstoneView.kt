package io.rippledown.cornerstone

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.constants.interpretation.NO_CORNERSTONES_TO_REVIEW
import io.rippledown.constants.interpretation.NO_CORNERSTONES_TO_REVIEW_LABEL

@Composable
fun CornerstoneView() {
    Text(
        text = NO_CORNERSTONES_TO_REVIEW,
        modifier = Modifier.padding(10.dp)
            .semantics { contentDescription = NO_CORNERSTONES_TO_REVIEW_LABEL }
    )
}