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
    fun startRuleToReplaceComment(toBeReplaced: String, replacement: String)
    fun startRuleToRemoveComment(comment: String)
}

@Composable
fun InterpretationActions(
    commentsGivenForCase: List<String>,
    allComments: Set<String>,
    handler: InterpretationActionsHandler
) {
    var addCommentDialogShowing by remember { mutableStateOf(false) }
    var replaceCommentDialogShowing by remember { mutableStateOf(false) }
    var removeCommentDialogShowing by remember { mutableStateOf(false) }
    val noDialogsShowing = !(addCommentDialogShowing || replaceCommentDialogShowing || removeCommentDialogShowing)

    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.wrapContentSize(BottomEnd)
    ) {
        ExtendedFloatingActionButton(
            onClick = { expanded = !expanded },
            text = { Text(text = CHANGE_INTERPRETATION) },
            icon = { Icon(Icons.Filled.Edit, CHANGE_INTERPRETATION_BUTTON) }
        )
        if (noDialogsShowing) {
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
                    enabled = true,
                    onClick = {
                        addCommentDialogShowing = true
                    }
                )
                InterpretationActionMenu(
                    text = REPLACE_COMMENT,
                    contentDescription = REPLACE_COMMENT_MENU,
                    iconResource = "replace_comment_24.png",
                    enabled = commentsGivenForCase.isNotEmpty(),
                    onClick = {
                        replaceCommentDialogShowing = true
                    }
                )
                InterpretationActionMenu(
                    text = REMOVE_COMMENT,
                    contentDescription = REMOVE_COMMENT_MENU,
                    iconResource = "remove_comment_24.png",
                    enabled = commentsGivenForCase.isNotEmpty(),
                    onClick = {
                        removeCommentDialogShowing = true
                    }
                )
            }
        }
    }
    if (addCommentDialogShowing) {
        AddCommentDialog(
            availableComments = allComments.toList() - commentsGivenForCase,
            handler = object : AddCommentHandler {
            override fun startRuleToAddComment(comment: String) {
                addCommentDialogShowing = false
                handler.startRuleToAddComment(comment)
            }

            override fun cancel() {
                addCommentDialogShowing = false
            }
        })
    }
    if (replaceCommentDialogShowing) {
        ReplaceCommentDialog(
            givenComments = commentsGivenForCase,
            availableComments = allComments.toList(),
            handler = object : ReplaceCommentHandler {
                override fun startRuleToReplaceComment(toBeReplaced: String, replacement: String) {
                    replaceCommentDialogShowing = false
                    handler.startRuleToReplaceComment(toBeReplaced, replacement)
                }

                override fun cancel() {
                    replaceCommentDialogShowing = false
                }
            })
    }

    if (removeCommentDialogShowing) {
        RemoveCommentDialog(
            givenComments = commentsGivenForCase,
            handler = object : RemoveCommentHandler {
                override fun startRuleToRemoveComment(comment: String) {
                    removeCommentDialogShowing = false
                    handler.startRuleToRemoveComment(comment)
                }

                override fun cancel() {
                    removeCommentDialogShowing = false
                }
            })
    }
}

@Composable
private fun InterpretationActionMenu(
    text: String,
    contentDescription: String,
    iconResource: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        onClick = onClick,
        enabled = enabled,
    ) {
        Row(verticalAlignment = CenterVertically) {
            Icon(
                contentDescription = contentDescription,
                painter = painterResource(iconResource),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
            )
        }
    }
}
