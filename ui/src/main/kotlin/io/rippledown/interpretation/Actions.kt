package io.rippledown.interpretation

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Composable
fun Actions() {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .wrapContentSize(Alignment.BottomEnd)
    ) {
        ExtendedFloatingActionButton(
            onClick = { expanded = !expanded },
            text = { Text(text = "Change interpretation") },
            icon = { Icon(Icons.Filled.Edit, "CHANGE_INTERPRETATION") }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(x = 20.dp, y = 50.dp),
            modifier = Modifier.width(400.dp)

        ) {
            DropdownMenuItem(
                onClick = {},
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        contentDescription = "Add a comment",
                        painter = painterResource("add_comment_24.png"),
                    )
                    Spacer(modifier = Modifier.width(8.dp)) // Provide some spacing between the icon and the text
                    Text("Add a comment")
                }
            }

            DropdownMenuItem(
                onClick = {}
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        contentDescription = "Add a comment",
                        painter = painterResource("replace_comment_24.png"),
                    )
                    Spacer(modifier = Modifier.width(8.dp)) // Provide some spacing between the icon and the text
                    Text("Replace the selected comment")
                }
            }
            DropdownMenuItem(
                onClick = {}
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        contentDescription = "Add a comment",
                        painter = painterResource("remove_comment_24.png"),
                    )
                    Spacer(modifier = Modifier.width(8.dp)) // Provide some spacing between the icon and the text
                    Text("Remove the selected comment")
                }
            }
        }
    }
}
