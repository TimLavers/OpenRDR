package io.rippledown.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Enter
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Exit
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.constants.interpretation.CANCEL
import io.rippledown.constants.interpretation.OK

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Scrollbar(scrollState: ScrollState, modifier: Modifier) {
    val hoverOverScroll = remember { mutableStateOf(false) }

    VerticalScrollbar(
        modifier = Modifier
            .then(modifier)
            .onPointerEvent(Enter) {
                hoverOverScroll.value = true
            }
            .onPointerEvent(Exit) {
                hoverOverScroll.value = false
            }
            .requiredWidth(if (hoverOverScroll.value) 10.dp else 5.dp),
        adapter = rememberScrollbarAdapter(scrollState)
    )
}

@Composable
fun OKCancelButtons(
    prefix: String,
    oKButtonEnabled: Boolean,
    onOK: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.End,
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
    ) {
        TextButton(
            onClick = onCancel,
            modifier = Modifier.semantics { contentDescription = "$prefix$CANCEL" }
        ) {
            Text(CANCEL)
        }
        Spacer(modifier = Modifier.width(8.dp))
        TextButton(
            enabled = oKButtonEnabled,
            onClick = onOK,
            modifier = Modifier.semantics { contentDescription = "$prefix$OK" }
        ) {
            Text(OK)
        }

    }
}

