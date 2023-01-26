import io.rippledown.model.Interpretation
import mui.lab.TreeItem
import mui.lab.TreeView
import react.FC
import react.Props
import react.ReactNode

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



