import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.rule.RuleSummary
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import mui.icons.material.ExpandMore
import mui.lab.MuiTreeView
import mui.lab.TreeItem
import mui.lab.TreeItemProps
import mui.lab.TreeView
import mui.material.Button
import mui.material.ListItemText
import react.*
import react.dom.client.createRoot

external interface ConclusionsViewHandler : Handler {
    var interpretation: Interpretation
}

val ConclusionsView = FC<ConclusionsViewHandler> { handler ->
    val interpretation = handler.interpretation

    TreeView {
        interpretation.conclusions().forEach { conclusion ->
            TreeItem {
                label = conclusion.text.unsafeCast<ReactNode>()
                nodeId = conclusion.text
                TreeItem {
                    label = "Condition 1 for ${conclusion.text}".unsafeCast<ReactNode>()
                    nodeId = "${conclusion.text}.1"
                }
                TreeItem {
                    label = "Condition 2 for ${conclusion.text}".unsafeCast<ReactNode>()
                    nodeId = "${conclusion.text}.2"
                    label = ListItemText.create{
                        +"condition 2"
                    }
                }*/
                }
            }
        }
}



