@file:OptIn(ExperimentalFoundationApi::class)

package io.rippledown.interpretation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val TOOLTIP_AREA_CONTENT_DESCRIPTION = "TOOLTIP_AREA"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ToolTipForIconAndLabel(
    toolTipText: String,
    labelText: String,
    iconContentDescription: String,
    isSelected: Boolean,
    icon: Painter,
    onClick: () -> Unit
) {
    TooltipArea(
        modifier = Modifier
            .semantics { contentDescription = TOOLTIP_AREA_CONTENT_DESCRIPTION },

        tooltip = {
            Surface(
                color = Color.Blue,
                shape = RoundedCornerShape(5.dp)
            ) {
                Text(
                    text = toolTipText,
                    modifier = Modifier.padding(5.dp),
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        },
        tooltipPlacement = TooltipPlacement.ComponentRect(
            alignment = Alignment.BottomCenter,
            offset = DpOffset(0.dp, 5.dp)
        ),
        content = {
            Row(
                modifier = Modifier
                    .height(30.dp)
                    .clickable {
                        onClick()
                    }
                    .background(color = Color.White),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    icon,
                    contentDescription = iconContentDescription,
                    tint = Color.Black,
                    modifier = Modifier
                        .height(25.dp)
                        .width(25.dp)
                        .padding(5.dp)
                )
                Text(
                    text = labelText,
                    modifier = Modifier.padding(5.dp),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colors.primary else Color.Black,
                    fontSize = 14.sp,
                )
            }
        }
    )
}