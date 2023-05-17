package io.rippledown.interpretation

import Handler
import io.rippledown.constants.interpretation.CONDITION_SELECTOR_ROW
import io.rippledown.model.condition.Condition
import mui.material.*
import mui.material.ButtonVariant.Companion.contained
import mui.system.sx
import react.FC
import react.ReactNode
import react.create
import react.useState
import web.cssom.Display
import web.cssom.JustifyContent.Companion.flexStart


external interface ConditionSelectorHandler : Handler {
    var conditionHints: List<Condition>
    var onDone: (conditionsPicked: List<Condition>) -> Unit
    var onCancel: () -> Unit
}

val ConditionSelector = FC<ConditionSelectorHandler> { handler ->
    val selectedConditions by useState<MutableList<Condition>>(mutableListOf())

    fun CheckBoxControl(index: Int) = FC<CheckboxProps> { props ->
        Checkbox {
            onChange = { _, checked ->
                if (checked) {
                    selectedConditions.add(handler.conditionHints[index])
                } else {
                    selectedConditions.remove(handler.conditionHints[index])
                }
            }
        }
    }.create()

    FormGroup {
        handler.conditionHints.forEachIndexed { index, condition ->
            FormControlLabel {
                id = "$CONDITION_SELECTOR_ROW$index"
                control = CheckBoxControl(index)
                label = condition.asText().unsafeCast<ReactNode>()
            }
        }
    }
    Box {
        sx {
            display = Display.flex
            justifyContent = flexStart
        }
        Button {
            id = "condition_picker_done_button"
            title = "Complete the rule with the selected conditions"
            variant = contained
            onClick = {
                handler.onDone(selectedConditions)
            }
            +"Done"
        }
        Button {
            id = "condition_picker_cancel_button"
            onClick = {
                handler.onCancel()
            }
            +"Cancel"
        }
    }
}