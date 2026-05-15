package io.rippledown.casecontrol

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rippledown.constants.caseview.CASE_VIEW_FILTER_CLEAR_DESCRIPTION
import io.rippledown.constants.caseview.CASE_VIEW_FILTER_FIELD_DESCRIPTION
import io.rippledown.constants.caseview.CASE_VIEW_FILTER_PLACEHOLDER

/**
 * Slim case-view filter widget. Owned by [CaseControl] and rendered above the
 * case panels, right-aligned, so a single filter applies uniformly to the
 * current case and any cornerstone shown beside it without visually competing
 * with the case data.
 *
 * Built on [BasicTextField] inside a [Surface] so the height (26dp) and width
 * (100dp) can be tightly constrained — Material's default `OutlinedTextField`
 * enforces a minimum height around 56dp which would dominate the chrome.
 *
 * The outline matches the neutral grey used by the interpretation panel's
 * `OutlinedCard` (material3 `colorScheme.outline`).
 *
 * Discoverability/affordance rules:
 *  - The magnifier icon is always shown as a hint.
 *  - The placeholder ("Filter") is shown only while the value is empty.
 *  - The clear (×) button appears only while the value is non-empty.
 */
@Composable
fun CaseViewFilterField(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline),
            color = MaterialTheme.colors.surface,
            modifier = Modifier.width(100.dp).height(26.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = LocalContentColor.current.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                val textStyle = TextStyle(
                    fontSize = 12.sp,
                    color = LocalContentColor.current
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = CASE_VIEW_FILTER_PLACEHOLDER,
                            style = textStyle.copy(color = LocalContentColor.current.copy(alpha = 0.5f))
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        textStyle = textStyle,
                        cursorBrush = SolidColor(MaterialTheme.colors.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            // Escape clears a non-empty filter; consumed only
                            // when there's something to clear so it does not
                            // swallow Escape from any enclosing dialogs.
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown &&
                                    event.key == Key.Escape &&
                                    value.isNotEmpty()
                                ) {
                                    onClear()
                                    true
                                } else {
                                    false
                                }
                            }
                            .semantics { contentDescription = CASE_VIEW_FILTER_FIELD_DESCRIPTION }
                    )
                }
                if (value.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(20.dp).semantics {
                            contentDescription = CASE_VIEW_FILTER_CLEAR_DESCRIPTION
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = null,
                            tint = LocalContentColor.current.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
