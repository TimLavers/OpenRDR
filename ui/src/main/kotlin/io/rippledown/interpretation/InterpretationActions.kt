package io.rippledown.interpretation

import androidx.compose.foundation.layout.*
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.rippledown.constants.interpretation.*
import openrdr.ui.generated.resources.Res
import openrdr.ui.generated.resources.add_comment_24
import openrdr.ui.generated.resources.remove_comment_24
import openrdr.ui.generated.resources.replace_comment_24
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

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
    val showDropdownMenu = !(addCommentDialogShowing || replaceCommentDialogShowing || removeCommentDialogShowing)
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.wrapContentSize(BottomEnd)
    ) {
        ChangeInterpretationIcon {
            expanded = !expanded
        }
        if (showDropdownMenu) {
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
                    iconResource = Res.drawable.add_comment_24,
                    enabled = true,
                    onClick = {
                        addCommentDialogShowing = true
                    }
                )
                InterpretationActionMenu(
                    text = REPLACE_COMMENT,
                    contentDescription = REPLACE_COMMENT_MENU,
                    iconResource = Res.drawable.replace_comment_24,
                    enabled = commentsGivenForCase.isNotEmpty(),
                    onClick = {
                        replaceCommentDialogShowing = true
                    }
                )
                InterpretationActionMenu(
                    text = REMOVE_COMMENT,
                    contentDescription = REMOVE_COMMENT_MENU,
                    iconResource = Res.drawable.remove_comment_24,
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
                expanded = false
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
                    expanded = false
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
                    expanded = false
                }
            })
    }
}

@Composable
private fun InterpretationActionMenu(
    text: String,
    contentDescription: String,
    iconResource: DrawableResource,
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
