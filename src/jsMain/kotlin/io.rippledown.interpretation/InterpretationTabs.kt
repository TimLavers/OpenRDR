package io.rippledown.interpretation

import Handler
import csstype.pc
import io.rippledown.constants.interpretation.INTERPRETATION_PANEL_CHANGES
import io.rippledown.constants.interpretation.INTERPRETATION_PANEL_ORIGINAL
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_CHANGES
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_ORIGINAL
import io.rippledown.model.Interpretation
import io.rippledown.model.diff.Diff
import kotlinx.coroutines.launch
import mui.lab.TabContext
import mui.lab.TabPanel
import mui.material.Box
import mui.material.Tab
import mui.material.Tabs
import mui.system.sx
import react.FC
import react.ReactNode
import react.useState

external interface InterpretationTabsHandler : Handler {
    var interpretation: Interpretation
}

val InterpretationTabs = FC<InterpretationTabsHandler> { handler ->
    var selectedTab by useState("0")
    var diffs by useState(listOf<Diff>())

    Box {
        sx {
            width = 50.pc
        }
        id = "interpretation_tabs"
        TabContext {
            value = selectedTab

            Tabs {
                value = selectedTab
                onChange = { _, value ->
                    selectedTab = value
                    handler.scope.launch {
                        val diffList = handler.api.interpretationChanges(handler.interpretation.caseId.name)
                        diffs = diffList.diffs
                    }
                }

                Tab {
                    id = INTERPRETATION_TAB_ORIGINAL
                    label = "Interpretation".unsafeCast<ReactNode>()
                    value = "0"
                }
                Tab {
                    id = INTERPRETATION_TAB_CHANGES
                    label = "Changes".unsafeCast<ReactNode>()
                    value = "1"
                }
            }

            TabPanel {
                value = "0"
                id = INTERPRETATION_PANEL_ORIGINAL
                InterpretationView {
                    api = handler.api
                    scope = handler.scope
                    interpretation = handler.interpretation
                }

            }

            TabPanel {
                value = "1"
                id = INTERPRETATION_PANEL_CHANGES
                DiffViewer {
                    api = handler.api
                    scope = handler.scope
                    changes = diffs
                }
            }

        }
    }

}

