package io.rippledown.interpretation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.rippledown.constants.interpretation.INTERPRETATION_PANEL_CONCLUSIONS
import io.rippledown.model.interpretationview.ViewableInterpretation


@Composable
fun ConclusionsView(interpretation: ViewableInterpretation) {
    Box(modifier = Modifier.semantics { contentDescription = INTERPRETATION_PANEL_CONCLUSIONS }) {
        TreeNodeView(interpretation.toTreeNode())
    }
}

fun ViewableInterpretation.toTreeNode(): TreeNode {
    val root = TreeNode()

    conclusions().forEach { conclusion ->
        val conclusionNode = TreeNode(conclusion.text)
        conditionsForConclusion(conclusion).forEach { condition ->
            conclusionNode.add(TreeNode(condition))
        }
        root.add(conclusionNode)
    }
    return root
}