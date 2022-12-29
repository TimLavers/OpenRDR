import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.rule.RuleSummary
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import mui.lab.TreeItem
import mui.lab.TreeView
import react.FC
import react.ReactNode
import react.create
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
                }
            }
        }
    }
}


fun main() {
    document.getElementById("root")?.let { container ->
        val text = "Go to Bondi now!"
        val interpretation = Interpretation().apply {
            (1..10).forEach {
                add(RuleSummary(conclusion = Conclusion("$it $text")))
            }
        }
        val ui = ConclusionsView.create {
            api = Api()
            scope = MainScope()
            this.interpretation = interpretation
        }
        createRoot(container.unsafeCast<dom.Element>()).render(ui)
    }
}
