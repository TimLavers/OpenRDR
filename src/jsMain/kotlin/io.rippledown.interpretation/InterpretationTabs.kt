package io.rippledown.interpretation

import Handler
import io.rippledown.constants.interpretation.INTERPRETATION_PANEL_CHANGES
import io.rippledown.constants.interpretation.INTERPRETATION_PANEL_ORIGINAL
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_CHANGES
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_ORIGINAL
import io.rippledown.model.Interpretation
import mui.lab.TabContext
import mui.lab.TabPanel
import mui.material.Box
import mui.material.Tab
import mui.material.Tabs
import mui.material.Typography
import mui.system.sx
import npm.BadgeWithChild
import npm.BadgeWithChildProps
import react.FC
import react.Props
import react.create
import react.useState
import web.cssom.pc
import web.cssom.px

external interface InterpretationTabsHandler : Handler {
    var interpretation: Interpretation
}

val InterpretationTabs = FC<InterpretationTabsHandler> { handler ->
    var selectedTab by useState("0")
    var interp by useState(handler.interpretation)

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
                }

                Tab {
                    id = INTERPRETATION_TAB_ORIGINAL
                    label = interpretationLabel
                    value = "0"
                }

                Tab {
                    id = INTERPRETATION_TAB_CHANGES
                    label = FC<BadgeWithChildProps> {
                        BadgeWithChild {
                            count = interp.numberOfChanges()
                            childNode = changesLabel
                        }
                    }.create()
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
                    onInterpretationEdited = { editedInterp ->
                        interp = editedInterp
                    }
                }
            }

            TabPanel {
                value = "1"
                id = INTERPRETATION_PANEL_CHANGES
                DiffViewer {
                    api = handler.api
                    scope = handler.scope
                    changes = interp.diffList.diffs
                }
            }
        }
    }
}

val interpretationLabel = FC<Props> {
    Typography {
        sx {
            fontSize = 14.px
            paddingTop = 10.px //space for the badge
        }
        +"Interpretation"
    }
}.create()

val changesLabel = FC<Props> {
    Typography {
        sx {
            fontSize = 14.px
            paddingTop = 10.px //space for the badge
            paddingRight = 10.px
        }
        +"Changes"
    }
}.create()



