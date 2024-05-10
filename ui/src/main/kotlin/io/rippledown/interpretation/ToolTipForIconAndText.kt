@file:OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)

package io.rippledown.interpretation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val TOOLTIP_AREA_CONTENT_DESCRIPTION = "TOOLTIP_AREA"
val TOOLTIP_TEXT_CONTENT_DESCRIPTION = "TOOLTIP_TEXT"
val BADGE_CONTENT_DESCRIPTION = "BADGE"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ToolTipForIconAndLabel(
    toolTipText: String,
    labelText: String = "",
    isSelected: Boolean,
    icon: Painter,
    onClick: () -> Unit,
    badgeCount: Int = 0,
    iconContentDescription: String = ""
) {
    TooltipArea(
        modifier = Modifier
            .semantics { contentDescription = TOOLTIP_AREA_CONTENT_DESCRIPTION },
        tooltip = {
            Surface(
                color = Color.White,
                border = BorderStroke(1.dp, Color.Black),
                shape = RoundedCornerShape(5.dp)
            ) {
                Text(
                    text = toolTipText,
                    modifier = Modifier.padding(5.dp)
                        .semantics { contentDescription = TOOLTIP_TEXT_CONTENT_DESCRIPTION },
                    fontSize = 12.sp,
                    color = Color.Black
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
                    .height(50.dp)
                    .clickable {
                        onClick()
                    }
                    .background(color = Color.White),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    Icon(
                        painter = icon,
                        contentDescription = iconContentDescription,
                        tint = Color.Black,
                        modifier = Modifier
                            .height(30.dp)
                            .width(30.dp)
                            .padding(5.dp)
                    )
                    //the optional badge.
                    if (badgeCount > 0) {
                        Text(
                            text = badgeCount.toString(),
                            color = Color.White,
                            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 10.dp, y = -10.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .semantics { contentDescription = BADGE_CONTENT_DESCRIPTION }
                        )
                    }
                }
                Text(
                    text = labelText,
                    modifier = Modifier.padding(start = 10.dp),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colors.primary else Color.Black,
                    fontSize = 14.sp,
                )
            }
        }
    )
}