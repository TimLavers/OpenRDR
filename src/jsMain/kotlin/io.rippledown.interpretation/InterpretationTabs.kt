package io.rippledown.interpretation

import io.rippledown.constants.interpretation.INTERPRETATION_CHANGES_BADGE
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_CHANGES
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_CONCLUSIONS
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_ORIGINAL
import io.rippledown.model.diff.Diff
import io.rippledown.model.interpretationview.ViewableInterpretation
import kotlinx.coroutines.launch
import main.Handler
import mui.lab.TabContext
import mui.lab.TabPanel
import mui.material.*
import mui.material.BadgeColor.Companion.primary
import mui.system.sx
import react.*
import web.cssom.pc
import web.cssom.px

external interface InterpretationTabsHandler : Handler {
    var interpretation: ViewableInterpretation
    var onStartRule: (selectedDiff: Diff) -> Unit
    var isCornerstone: Boolean
}

val InterpretationTabs = FC<InterpretationTabsHandler> { handler ->
    var selectedTab by useState("0")
    var interp by useState(handler.interpretation)

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
                    label = badgeFor(interp.numberOfChanges())
                    value = "2"
                }
            }

            TabPanel {
                value = "0"
                key = interpretationViewKey(interp.latestText())
                sx {
                    padding = 0.px
                }

                InterpretationView {
                    api = handler.api
                    scope = handler.scope
                    text = interp.latestText()
                    onEdited = { changedText ->
                        handler.scope.launch {
                            interp.verifiedText = changedText
                            interp = handler.api.saveVerifiedInterpretation(interp)
                        }
                    }
                    isCornerstone = handler.isCornerstone
                }
            }

            TabPanel {
                value = "1"
                sx {
                    padding = 0.px
                }
                ConclusionsView {
                    interpretation = interp
                }
            }

            TabPanel {
                value = "2"
                sx {
                    padding = 0.px
                }
                DiffViewer {
                    diffList = interp.diffList
                    onStartRule = { selectedDiff ->
                        handler.onStartRule(diffList.diffs[selectedDiff])
                    }
                }
            }
        }
    }
}

fun interpretationViewKey(text: String) = text

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

private fun badgeFor(count: Int) = FC<BadgeProps> {
    Badge {
        id = INTERPRETATION_CHANGES_BADGE
        color = primary
        showZero = false
        badgeContent = count.unsafeCast<ReactNode>()
        +changesLabel
    }
}.create()

