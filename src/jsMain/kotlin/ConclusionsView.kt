import io.rippledown.model.Interpretation
import mui.icons.material.ExpandLess
import mui.icons.material.ExpandMore
import mui.lab.TreeItem
import mui.lab.TreeView
import react.FC
import react.Props
import react.ReactNode
import react.createElement

external interface ConclusionsViewHandler : Props {
    var interpretation: Interpretation
}

val ConclusionsView = FC<ConclusionsViewHandler> { handler ->
    val interpretation = handler.interpretation

    TreeView {
        interpretation.conclusions().forEach { conclusion ->
            TreeItem {
                label = conclusion.text.unsafeCast<ReactNode>()
                nodeId = conclusion.text
                expandIcon = createElement(ExpandMore)
                collapseIcon = createElement(ExpandLess)
                interpretation.conditionsForConclusion(conclusion).forEach { condition ->
                    TreeItem {
                        label = condition.unsafeCast<ReactNode>()
                        nodeId = condition
                    }
                }
            }
        }

    }
}



