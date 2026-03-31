package io.rippledown.interpretation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.constants.interpretation.DIFF_ROW_ADDITION
import io.rippledown.constants.interpretation.DIFF_ROW_REMOVAL
import io.rippledown.constants.interpretation.DIFF_ROW_REPLACEMENT_NEW
import io.rippledown.constants.interpretation.DIFF_ROW_REPLACEMENT_ORIGINAL
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Diff
import io.rippledown.model.diff.Removal
import io.rippledown.model.diff.Replacement

val DIFF_ADDITION_COLOR = Color(0xFFC8E6C9)
val DIFF_REMOVAL_COLOR = Color(0xFFFFCDD2)

@Composable
fun DiffRow(diff: Diff) {
    when (diff) {
        is Addition -> {
            Text(
                text = diff.addedText,
                modifier = Modifier
                    .background(DIFF_ADDITION_COLOR)
                    .padding(10.dp)
                    .semantics { contentDescription = DIFF_ROW_ADDITION }
            )
        }

        is Removal -> {
            Text(
                text = diff.removedText,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DIFF_REMOVAL_COLOR)
                    .padding(10.dp)
                    .semantics { contentDescription = DIFF_ROW_REMOVAL }
            )
        }

        is Replacement -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = diff.originalText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DIFF_REMOVAL_COLOR)
                        .padding(10.dp)
                        .semantics { contentDescription = DIFF_ROW_REPLACEMENT_ORIGINAL }
                )
                Text(
                    text = diff.replacementText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DIFF_ADDITION_COLOR)
                        .padding(10.dp)
                        .semantics { contentDescription = DIFF_ROW_REPLACEMENT_NEW }
                )
            }
        }
    }
}
