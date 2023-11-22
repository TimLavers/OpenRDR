package io.rippledown.kb

import io.rippledown.constants.kb.*
import io.rippledown.main.Handler
import kotlinx.coroutines.launch
import mui.material.*
import mui.material.FormControlMargin.Companion.dense
import mui.material.FormControlVariant.Companion.standard
import mui.material.styles.TypographyVariant.Companion.subtitle1
import react.FC
import react.ReactNode
import react.dom.events.FormEvent
import react.dom.onChange
import react.useState
import web.html.HTMLDivElement
import web.html.HTMLInputElement

external interface CreateKBHandler : Handler {
    var onFinish: () -> Unit
}

val CreateKB = FC<CreateKBHandler> { handler ->
    var isOpen by useState(false)
    var name by useState("")

    MenuItem {
        id = KB_CREATE_MENU_ITEM_ID
        ListItemText {
            primary = "New project".unsafeCast<ReactNode>()
        }
        onClick = {
            isOpen = true
        }
    }
    Dialog {
        id = KB_CREATE_DIALOG
        open = isOpen
        DialogTitle {
            Typography {
                variant = subtitle1
                +"Create new project"
            }
        }
        DialogContent {
            TextField {
                id = KB_CREATE_PROJECT_NAME_FIELD
                label = "Project name".unsafeCast<ReactNode>()
                variant = standard
                autoFocus = true
                fullWidth = true
                margin = dense
                size = Size.small
                onChange = { event: FormEvent<HTMLDivElement> ->
                    name = event.target.unsafeCast<HTMLInputElement>().value
                }
            }
        }
        DialogActions {
            Button {
                id = CANCEL_CREATE_BUTTON_ID
                onClick = {
                    isOpen = false
                    handler.onFinish()

                }
                +"Cancel"
            }
            Button {
                id = CONFIRM_CREATE_BUTTON_ID
                onClick = {
                    handler.scope.launch {
                        handler.api.createKB(name)
                    }
                    isOpen = false
                    handler.onFinish()
                }
                +"OK"
            }
        }
    }
}