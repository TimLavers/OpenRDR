package io.rippledown.interpretation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IconWithToolTip(
    toolTipText: String,
    icon: Painter,
    onClick: () -> Unit,
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
                    modifier = Modifier
                        .padding(5.dp)
                        .semantics { contentDescription = TOOLTIP_TEXT_CONTENT_DESCRIPTION },
                    fontSize = 12.sp,
                    color = Color.Black,

                    )
            }
        },
        tooltipPlacement = TooltipPlacement.ComponentRect(
            alignment = Alignment.BottomCenter,
            offset = DpOffset(0.dp, 5.dp)
        ),
        content = {
            Icon(
                painter = icon,
                contentDescription = iconContentDescription,
                tint = Color.Black,
                modifier = Modifier
                    .height(20.dp)
                    .onClick { onClick() }
            )
        }
    )
}