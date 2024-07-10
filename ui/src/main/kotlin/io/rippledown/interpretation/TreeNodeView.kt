package io.rippledown.interpretation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class TreeNode(val name: String = "", val children: MutableList<TreeNode> = mutableListOf()) {
    fun add(node: TreeNode) {
        children.add(node)
    }
}

@Composable
fun collapseIcon() = painterResource("icons8-collapse-30.png")

@Composable
fun expandIcon() = painterResource("icons8-expand-30.png")

const val TREE_ICON = "tree_icon"
const val TREE_TEXT = "tree_text"

@Composable
fun TreeNodeView(node: TreeNode, level: Int = 0, parentIndex: Int = 0, index: Int = 0) {
    var isExpanded by remember { mutableStateOf(true) }

    Column(modifier = Modifier.padding(start = 20.dp * level)) {
        if (level > 0) {
            Row {
                if (node.children.isNotEmpty()) {
                    Icon(
                        painter = if (isExpanded) collapseIcon() else expandIcon(),
                        contentDescription = "collapse/expand icon",
                        modifier = Modifier
                            .clickable { isExpanded = !isExpanded }
                            .size(16.dp)
                            .semantics {
                                contentDescription = iconContentDescription(level, parentIndex, index, node.name)
                            }
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(text = node.name,
                    fontWeight = if (level == 1) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                    modifier = Modifier.semantics {
                        contentDescription = textContentDescription(level, parentIndex, index, node.name)
                    })
            }
        }
        if (isExpanded) {
            node.children.forEachIndexed { childIndex, childNode ->
                TreeNodeView(node = childNode, level = level + 1, parentIndex = index, index = childIndex)
            }
        }
    }
}

fun iconContentDescription(
    level: Int,
    parentIndex: Int,
    index: Int,
    text: String
) = "$TREE_ICON $level $parentIndex $index $text"

fun textContentDescription(
    level: Int,
    parentIndex: Int,
    index: Int,
    text: String
) = "$TREE_TEXT $level $parentIndex $index $text"




