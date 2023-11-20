package io.rippledown.appbar

import io.rippledown.constants.kb.KB_SELECTOR_ID
import io.rippledown.constants.main.MAIN_HEADING
import io.rippledown.constants.main.MAIN_HEADING_ID
import io.rippledown.kb.CreateKB
import io.rippledown.kb.ExportKB
import io.rippledown.kb.ImportKB
import io.rippledown.main.Handler
import io.rippledown.main.blue
import io.rippledown.main.debug
import io.rippledown.main.white
import io.rippledown.model.KBInfo
import kotlinx.coroutines.launch
import mui.icons.material.Dehaze
import mui.material.*
import mui.material.IconButtonColor.Companion.inherit
import mui.material.IconButtonEdge.Companion.start
import mui.material.SelectVariant.Companion.standard
import mui.material.ToolbarVariant.Companion.dense
import mui.material.styles.TypographyVariant.Companion.h6
import mui.system.sx
import react.FC
import react.dom.events.ChangeEvent
import react.dom.events.MouseEvent
import react.useEffectOnce
import react.useState
import web.cssom.px
import web.html.HTMLElement
import web.html.HTMLInputElement

external interface AppBarHandler : Handler {
    var isRuleSessionInProgress: Boolean
}

var ApplicationBar = FC<AppBarHandler> { handler ->
    var kbInfo: KBInfo? by useState(null)
    var selectorIsOpen by useState(false)

    fun kbName() = if (kbInfo != null) kbInfo!!.name else ""

    fun reloadKB() {
        handler.scope.launch {
            kbInfo = handler.api.kbInfo()
        }
    }

    fun handleChange(event: ChangeEvent<HTMLInputElement>) {
        val value = event.target.value
        if (value == "new") {
            debug("TODO: create new KB")
        } else {
            debug("TODO: change KB to $value")
//            handler.scope.launch {
//                handler.api.setKB(value)
//                reloadKB()
//            }
        }
    }

    useEffectOnce {
        reloadKB()
    }

    AppBar {
        id = "app-bar"
        Toolbar {
            variant = dense
            IconButton {
                id = "dehaze-button_id"
                edge = start
                color = inherit
                onClick = {
                    //TODO open a drawer with other tools
                }
                Dehaze {
                }
            }

            Typography {
                id = MAIN_HEADING_ID
                variant = h6
                sx {
                    marginLeft = 20.px
                    marginRight = 100.px
                }
                +MAIN_HEADING
            }
            FormControl {
                Select {
                    id = KB_SELECTOR_ID
                    variant = standard
                    disableUnderline = true
                    disabled = handler.isRuleSessionInProgress
                    value = kbName()
                    open = selectorIsOpen
                    onChange = { event: ChangeEvent<HTMLInputElement>, _ ->
                        handleChange(event)
                    }
                    onClick = { event: MouseEvent<HTMLElement, *> ->
                        val target = event.target as HTMLElement
                        if (target.id == KB_SELECTOR_ID) {
                            selectorIsOpen = !selectorIsOpen
                        }
                    }
                    sx {
                        color = white
                    }
                    CreateKB {
                        api = handler.api
                        scope = handler.scope
                        onFinish = {
                            reloadKB()
                            selectorIsOpen = false
                        }
                    }
                    ImportKB {
                        api = handler.api
                        scope = handler.scope
                        reloadKB = {
                            reloadKB()
                            selectorIsOpen = false
                        }
                    }
                    ExportKB {
                        api = handler.api
                        scope = handler.scope
                        onFinish = {
                            selectorIsOpen = false
                        }
                    }
                    Divider {
                        textAlign = DividerTextAlign.left
                        light = true
                        sx {
                            marginTop = 10.px
                            marginBottom = 10.px
                            color = blue
                        }
                        +"Recent projects"
                    }
                    MenuItem {
                        value = kbName()
                        onClick = {
                            selectorIsOpen = false
                        }
                        +kbName()
                    }

                    MenuItem {
                        value = "bo"
                        onClick = {
                            selectorIsOpen = false
                        }
                        +"Bondi"
                    }
                    MenuItem {
                        value = "ma"
                        onClick = {
                            selectorIsOpen = false
                        }
                        +"Malabar"
                    }
                    MenuItem {
                        value = "co"
                        onClick = {
                            selectorIsOpen = false
                        }
                        +"Coogee"
                    }
                }
            }
        }
    }
}
