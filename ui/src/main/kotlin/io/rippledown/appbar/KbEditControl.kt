package io.rippledown.appbar

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role.Companion.Button
import androidx.compose.ui.semantics.Role.Companion.DropdownList
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign.Companion.Start
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import io.rippledown.constants.kb.*
import io.rippledown.constants.main.*
import io.rippledown.model.KBInfo
import io.rippledown.sample.SampleKB
import java.io.File

interface KbEditControlHandler {
    var selectKB: (id: String) -> Unit
    var createKB: (name: String) -> Unit
    var createKBFromSample: (name: String, sample: SampleKB) -> Unit
    var importKB: (data: File) -> Unit
    var exportKB: (data: File) -> Unit
    val kbList: () -> List<KBInfo>
}

@Composable
@Preview
fun KbEditControl(kbInfo: KBInfo) {
    var expanded by remember { mutableStateOf(false) }
    var createKbDialogShowing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
    }

    Box (
        Modifier
            .semantics {
                contentDescription = "KB Edit Control"
            }
            .padding(16.dp)
            .testTag(KB_CONTROL_ID)
    ) {

        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.semantics {
                contentDescription = "KB Edit Control Button"
            }
        ) {
            Icon(
                imageVector = Default.Edit,
                contentDescription = "KB Edit Control Button Icon",
                tint = colors.onPrimary
            )
        }
    }
}
