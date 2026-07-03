package io.rippledown.interpretation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rippledown.constants.interpretation.REPORT_PANEL
import io.rippledown.constants.interpretation.REPORT_TEXT
import io.rippledown.constants.interpretation.REPORT_TOGGLE

@Composable
fun ReportView(
    reportText: String?,   // null = not yet loaded / loading
    isVisible: Boolean,
    onToggle: (Boolean) -> Unit,
    isLoading: Boolean = false,
    hasComments: Boolean = true, // false when the case has no comments to report on
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .clickable { onToggle(!isVisible) }
                .semantics { contentDescription = REPORT_TOGGLE },
        ) {
            Icon(
                imageVector = if (isVisible) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.DarkGray,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Report",
                color = Color.DarkGray,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        if (isVisible) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
                    .semantics { contentDescription = REPORT_PANEL }) {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(8.dp)) {
                    when {
                        isLoading -> Text("Generating report…")
                        !hasComments -> Text("No comments to report on.")
                        reportText.isNullOrBlank() -> Text("No report.")
                        else -> Text(
                            text = reportText,
                            modifier = Modifier.semantics { contentDescription = REPORT_TEXT })
                    }
                }
            }
        }
    }
}
