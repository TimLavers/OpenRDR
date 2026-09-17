package io.rippledown.chat

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CapabilityCard(message: CapabilityListMessage, index: Int, maxHeight: Dp) {
    val scroll = rememberScrollState()
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight)
            .testTag("CAPABILITY_CARD")
            .semantics { contentDescription = "$BOT$index:${message.text}" }
    ) {
        Box(Modifier.padding(12.dp)) {
            Column(
                Modifier.fillMaxWidth().padding(end = 12.dp).verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                message.sections.forEach { section ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            section.heading,
                            color = Color.DarkGray,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.semantics { heading() }
                        )
                        section.items.forEach { item ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("\u2022", color = Color.DarkGray, fontSize = 13.sp)
                                Text(item, color = Color.Black, fontSize = 13.sp, lineHeight = 17.sp)
                            }
                        }
                    }
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(scroll),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                    .testTag("CAPABILITY_SCROLLBAR")
            )
        }
    }
}
