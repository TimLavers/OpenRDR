package io.rippledown.interpretation

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.rippledown.constants.interpretation.*

interface InterpretationActionsHandler {
    fun startRuleToAddComment(comment: String)
    fun replaceComment()
    fun removeComment()
}

@Composable
fun InterpretationActions(handler: InterpretationActionsHandler) {
    var addCommentDialogShowing by remember { mutableStateOf(false) }

    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.wrapContentSize(BottomEnd)
    ) {
        ExtendedFloatingActionButton(
            onClick = { expanded = !expanded },
            text = { Text(text = CHANGE_INTERPRETATION) },
            icon = { Icon(Icons.Filled.Edit, CHANGE_INTERPRETATION_BUTTON) }
        )
        if (!addCommentDialogShowing) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = DpOffset(x = 20.dp, y = 50.dp),
                modifier = Modifier
                    .width(400.dp)
                    .semantics { contentDescription = CHANGE_INTERPRETATION_DROPDOWN }

            ) {
                InterpretationActionMenu(
                    text = ADD_COMMENT,
                    contentDescription = ADD_COMMENT_MENU,
                    iconResource = "add_comment_24.png",
                    onClick = { addCommentDialogShowing = true }
                )
                InterpretationActionMenu(
                    text = REPLACE_COMMENT,
                    contentDescription = REPLACE_COMMENT_MENU,
                    iconResource = "replace_comment_24.png",
                    onClick = { handler.replaceComment() }
                )
                InterpretationActionMenu(
                    text = REMOVE_COMMENT,
                    contentDescription = REMOVE_COMMENT_MENU,
                    iconResource = "remove_comment_24.png",
                    onClick = { handler.removeComment() }
                )
            }
        }
    }
    AddCommentDialog(isShowing = addCommentDialogShowing, handler = object : AddCommentHandler {
        override fun startRuleToAddComment(comment: String) {
            addCommentDialogShowing = false
            handler.startRuleToAddComment(comment)
        }

        override fun cancel() {
            addCommentDialogShowing = false
        }
    })
}

@Composable
private fun InterpretationActionMenu(
    text: String,
    contentDescription: String,
    iconResource: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        onClick = onClick,
    ) {
        Row(verticalAlignment = CenterVertically) {
            Icon(
                contentDescription = contentDescription,
                painter = painterResource(iconResource),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text)
        }
    }
}
