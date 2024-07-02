package io.rippledown.interpretation

import androidx.compose.foundation.layout.Box
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.rippledown.constants.interpretation.INTERPRETATION_PANEL_CONCLUSIONS
import io.rippledown.constants.interpretation.NO_CONCLUSIONS
import io.rippledown.model.interpretationview.ViewableInterpretation


@Composable
fun ConclusionsView(interpretation: ViewableInterpretation) {
    Box(modifier = Modifier.semantics { contentDescription = INTERPRETATION_PANEL_CONCLUSIONS }) {
        if (interpretation.conclusions().isNotEmpty()) {
            TreeNodeView(interpretation.toTreeNode())
        } else {
            Text(
                text = "There are no comments given for this case",
                modifier = Modifier.semantics { contentDescription = NO_CONCLUSIONS })
        }
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