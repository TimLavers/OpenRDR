package io.rippledown.interpretation

import Handler
import io.rippledown.constants.interpretation.*
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
    var conditions: List<Condition>
    var onDone: (conditionsPicked: List<Condition>) -> Unit
    var changedConditions: (conditionsPicked: List<Condition>) -> Unit
    var onCancel: () -> Unit
}

val ConditionSelector = FC<ConditionSelectorHandler> { handler ->
    val selectedConditions by useState<MutableList<Condition>>(mutableListOf())

    fun CheckBoxControl(index: Int) = FC<CheckboxProps> { _ ->
        Checkbox {
            id = "$CONDITION_SELECTOR_CHECKBOX$index"
            onChange = { _, checked ->
                if (checked) {
                    selectedConditions.add(handler.conditions[index])
                } else {
                    selectedConditions.remove(handler.conditions[index])
                }
                handler.changedConditions(selectedConditions)
            }
        }
    }.create()

    FormGroup {
        handler.conditions.forEachIndexed { index, condition ->
            FormControlLabel {
                id = "$CONDITION_SELECTOR_ROW$index"
                control = CheckBoxControl(index)
                label = condition.asText().unsafeCast<ReactNode>()
            }
        }
    }
    Box {
        id = CONDITION_SELECTOR_BUTTONS
        sx {
            display = Display.flex
            justifyContent = flexStart
        }
        Button {
            id = CONDITION_SELECTOR_DONE_BUTTON
            title = "Complete the rule with the selected conditions"
            variant = contained
            onClick = {
                handler.onDone(selectedConditions)
            }
            +"Done"
        }
        Button {
            id = CONDITION_SELECTOR_CANCEL_BUTTON
            onClick = {
                handler.onCancel()
            }
            +"Cancel"
        }
    }
}