package io.rippledown.interpretation

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.junit.Rule
import org.junit.Test

class TreeNodeViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()


    @Test
    fun `should show tree node view`() {
        with(composeTestRule) {
            setContent {
                treeNodeView()
            }
            requireNodeText(1, 0, 0, "Child 1")
            requireNodeText(1, 0, 1, "Child 2")
            requireNodeText(2, 0, 0, "Grandchild 1")
            requireNodeText(2, 0, 1, "Grandchild 2")
            requireNodeText(2, 1, 0, "Grandchild 3")
            requireNodeText(2, 1, 1, "Grandchild 4")
        }
    }

    @Test
    fun `should collapse a node`() {
        with(composeTestRule) {
            setContent {
                treeNodeView()
            }
            clickNode(1, 0, 0, "Child 1")
            requireNodeText(1, 0, 0, "Child 1")
            requireNodeTextNotShowing(2, 0, 0, "Grandchild 1")
            requireNodeTextNotShowing(2, 0, 1, "Grandchild 2")
            requireNodeText(1, 0, 1, "Child 2")
            requireNodeText(2, 1, 0, "Grandchild 3")
            requireNodeText(2, 1, 1, "Grandchild 4")
        }
    }

    @Test
    fun `should expand a node`() {
        with(composeTestRule) {
            setContent {
                treeNodeView()
            }
            //Given
            clickNode(1, 0, 1, "Child 2")
            requireNodeText(1, 0, 0, "Child 1")
            requireNodeText(2, 0, 0, "Grandchild 1")
            requireNodeText(2, 0, 1, "Grandchild 2")
            requireNodeText(1, 0, 1, "Child 2")
            requireNodeTextNotShowing(2, 1, 0, "Grandchild 3")
            requireNodeTextNotShowing(2, 1, 1, "Grandchild 4")

            //When
            clickNode(1, 0, 1, "Child 2")

            //Then
            requireNodeText(2, 1, 0, "Grandchild 3")
            requireNodeText(2, 1, 1, "Grandchild 4")
        }
    }
}

@Composable
fun treeNodeView() = TreeNodeView(
    TreeNode(
        "Root",
        mutableListOf(
            TreeNode(
                "Child 1",
                mutableListOf(
                    TreeNode("Grandchild 1"),
                    TreeNode("Grandchild 2")
                )
            ),
            TreeNode(
                name = "Child 2",
                mutableListOf(
                    TreeNode("Grandchild 3"),
                    TreeNode("Grandchild 4")
                )
            )
        )
    )
)

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
        ) {
            treeNodeView()
        }
    }
}