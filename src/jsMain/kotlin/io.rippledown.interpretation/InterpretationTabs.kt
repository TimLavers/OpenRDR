package io.rippledown.interpretation

import Handler
import io.rippledown.constants.interpretation.INTERPRETATION_CHANGES_BADGE
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_CHANGES
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_CONCLUSIONS
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_ORIGINAL
import io.rippledown.model.Interpretation
import mui.lab.TabContext
import mui.lab.TabPanel
import mui.material.*
import mui.material.BadgeColor.Companion.primary
import mui.system.sx
import react.*
import web.cssom.pc
import web.cssom.px

external interface InterpretationTabsHandler : Handler {
    var interpretation: Interpretation
    var refreshCase: () -> Unit
    var onStartRule: (interp: Interpretation) -> Unit
    var isCornerstone: Boolean
}

val InterpretationTabs = FC<InterpretationTabsHandler> { handler ->
    var selectedTab by useState("0")

    Box {
        sx {
            width = 30.pc
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
                    id = INTERPRETATION_TAB_CONCLUSIONS
                    label = conclusionsLabel
                    value = "1"
                }

                Tab {
                    id = INTERPRETATION_TAB_CHANGES
                    label = FC<BadgeProps> {
                        Badge {
                            id = INTERPRETATION_CHANGES_BADGE
                            color = primary
                            showZero = false
                            badgeContent = handler.interpretation.numberOfChanges().unsafeCast<ReactNode>()
                            +changesLabel
                        }
                    }.create()
                    value = "2"
                }
            }

            TabPanel {
                value = "0"

                //Force a re-render of the underlying TextField when the interpretation changes
                //See https://jaketrent.com/post/rerender-defaultvalue-value-change/
                key = handler.interpretation.latestText()

                InterpretationView {
                    api = handler.api
                    scope = handler.scope
                    text = handler.interpretation.latestText()
                    onEdited = {
                        handler.refreshCase()
                    }
                    isCornerstone = handler.isCornerstone
                }
            }

            TabPanel {
                value = "1"
                ConclusionsView {
                    interpretation = handler.interpretation
                }
            }

            TabPanel {
                value = "2"
                DiffViewer {
                    api = handler.api
                    scope = handler.scope
                    interpretation = handler.interpretation
                    onStartRule = { newInterpretation ->
                        handler.onStartRule(newInterpretation)
                    }
                }
            }
        }
    }
}

val interpretationLabel = FC<Props> {
    Typography {
        sx {
            fontSize = 14.px
            paddingTop = 10.px //align with the other tab
        }
        +"Interpretation"
    }
}.create()

val conclusionsLabel = FC<Props> {
    Typography {
        sx {
            fontSize = 14.px
            paddingTop = 10.px //align with the other tab
        }
        +"Conclusions"
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



