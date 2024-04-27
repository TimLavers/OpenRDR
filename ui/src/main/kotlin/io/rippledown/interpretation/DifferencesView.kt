package io.rippledown.interpretation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.rippledown.constants.interpretation.INTERPRETATION_PANEL_CHANGES
import io.rippledown.model.diff.Diff
import io.rippledown.model.diff.DiffList

interface DifferencesViewHandler {
    fun onStartRule(selectedDiff: Diff)
}

@Composable
fun DifferencesView(diffList: DiffList, handler: DifferencesViewHandler) {
    Box(modifier = Modifier.semantics { contentDescription = INTERPRETATION_PANEL_CHANGES }) {

    }
}