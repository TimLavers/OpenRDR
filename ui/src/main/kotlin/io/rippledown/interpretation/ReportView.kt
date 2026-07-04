package io.rippledown.interpretation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rippledown.constants.interpretation.*

@OptIn(ExperimentalFoundationApi::class)
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
            TooltipArea(
                tooltip = {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = REPORT_DISCLAIMER,
                            style = TextStyle(fontSize = 12.sp),
                            modifier = Modifier.widthIn(max = 400.dp).padding(8.dp)
                        )
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = Color.DarkGray.copy(alpha = 0.6f),
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(14.dp)
                        .semantics { contentDescription = REPORT_DISCLAIMER_ICON }
                )
            }
        }
        if (isVisible) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
                    .semantics { contentDescription = REPORT_PANEL },
                colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
            ) {
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
