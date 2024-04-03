package io.rippledown.interpretation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class TreeNode(val name: String, val children: List<TreeNode> = emptyList())

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

@ExperimentalAnimationApi
@Composable
fun Node(
    modifier: Modifier = Modifier,
    nodeModel: NodeModel
) {
    Column(
        horizontalAlignment = Alignment.Start
    ) {
        val isChildrenShown = remember { mutableStateOf(true) }

        NodeBox(
            modifier = modifier.clickable(onClick = {
                isChildrenShown.value = !isChildrenShown.value
            }),
            isExpanded = isChildrenShown.value
        )

        Spacer(modifier = Modifier.size(8.dp))

        AnimatedVisibility(visible = isChildrenShown.value) {
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.Center
            ) {
                nodeModel.children.forEachIndexed { index, model ->
                    Node(nodeModel = model)
                    if (index != nodeModel.children.size - 1) {
                        Spacer(modifier = Modifier.size(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun NodeBox(modifier: Modifier = Modifier, isExpanded: Boolean) {
    Box(
        modifier = modifier
            .size(8.dp)
            .background(if (isExpanded) Color.Green else Color.Blue),
    )
}


@ExperimentalAnimationApi
@Preview
@Composable
fun TreePreview() {
    Node(
        nodeModel = rootModel()
    )
}

data class NodeModel(val children: List<NodeModel> = emptyList())

private fun rootModel(): NodeModel {
    return NodeModel(
        listOf(
            NodeModel(
                listOf(
                    NodeModel(
                        listOf(
                            NodeModel(
                                listOf(
                                    NodeModel(
                                        listOf(
                                            NodeModel(
                                                listOf(
                                                    NodeModel(
                                                        listOf(
                                                            NodeModel(),
                                                            NodeModel(),
                                                            NodeModel(),
                                                        )
                                                    ),
                                                    NodeModel(
                                                        listOf(
                                                            NodeModel(),
                                                            NodeModel(),
                                                            NodeModel(),
                                                        )
                                                    ),
                                                )
                                            ),
                                            NodeModel(
                                                listOf(
                                                    NodeModel(),
                                                    NodeModel(),
                                                    NodeModel(),
                                                )
                                            ),
                                        )
                                    ),
                                    NodeModel(
                                        listOf(
                                            NodeModel(),
                                            NodeModel(),
                                            NodeModel(),
                                        )
                                    ),
                                )
                            ),
                            NodeModel(
                                listOf(
                                    NodeModel(),
                                    NodeModel(),
                                    NodeModel(),
                                )
                            ),
                        )
                    ),
                    NodeModel(
                        listOf(
                            NodeModel(),
                            NodeModel(),
                            NodeModel(),
                        )
                    ),
                    NodeModel(
                        listOf(
                            NodeModel(),
                            NodeModel(),
                            NodeModel(),
                        )
                    )
                )
            ),

            NodeModel(
                listOf(
                    NodeModel(
                        listOf(
                            NodeModel(
                                listOf(
                                    NodeModel(
                                        listOf(
                                            NodeModel(
                                                listOf(
                                                    NodeModel(
                                                        listOf(
                                                            NodeModel(),
                                                            NodeModel(),
                                                            NodeModel(),
                                                        )
                                                    ),
                                                    NodeModel(
                                                        listOf(
                                                            NodeModel(),
                                                            NodeModel(),
                                                            NodeModel(),
                                                        )
                                                    ),
                                                )
                                            ),
                                            NodeModel(
                                                listOf(
                                                    NodeModel(),
                                                    NodeModel(),
                                                    NodeModel(),
                                                )
                                            ),
                                        )
                                    ),
                                    NodeModel(
                                        listOf(
                                            NodeModel(),
                                            NodeModel(),
                                            NodeModel(),
                                        )
                                    ),
                                )
                            ),
                            NodeModel(
                                listOf(
                                    NodeModel(),
                                    NodeModel(),
                                    NodeModel(),
                                )
                            ),
                        )
                    ),
                    NodeModel(
                        listOf(
                            NodeModel(),
                            NodeModel(),
                            NodeModel(),
                        )
                    ),
                    NodeModel(
                        listOf(
                            NodeModel(),
                            NodeModel(),
                            NodeModel(),
                        )
                    )
                )
            ),
        )
    )
}

