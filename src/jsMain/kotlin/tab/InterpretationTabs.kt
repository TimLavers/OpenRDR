package tab

import Handler
import InterpretationView
import csstype.pc
import diffviewer.ReactDiffViewer
import io.rippledown.model.Interpretation
import mui.lab.TabContext
import mui.lab.TabPanel
import mui.material.Box
import mui.material.Tab
import mui.material.Tabs
import mui.system.sx
import react.FC
import react.ReactNode
import react.create
import react.useState

external interface InterpretationTabsHandler : Handler {
    var interpretation: Interpretation
}

val InterpretationTabs = FC<InterpretationTabsHandler> { handler ->
    var selectedTab by useState("0")

    Box {
        sx {
            width = 50.pc
        }
        TabContext {
            value = selectedTab

            Tabs {
                value = selectedTab
                onChange = { _, value ->
                    selectedTab = value
                }

                Tab {
                    label = "Interpretation".unsafeCast<ReactNode>()
                    value = "0"
                }
                Tab {
                    label = "Changes".unsafeCast<ReactNode>()
                    value = "1"
                }
            }

            TabPanel {
                value = "0"
                children = InterpretationView.create {
                    interpretation = handler.interpretation
                }

            }
            TabPanel {
                value = "1"
                children = ReactDiffViewer.create {
                    oldValue = "Go to Bondi now\nand bring your swimmers"
                    newValue = "Go to Bondi beach now\nnd bring your swimmers\nand flippers"
                }

            }
        }
    }

}

/*fun main() {
    document.getElementById("root")?.let { container ->
        val ui = InterpretationTabs.create {
            interpretation = Interpretation(CaseId("A", "case A"), "Go to Bondi")
        }
        createRoot(container.unsafeCast<Element>()).render(ui)
    }
}*/
