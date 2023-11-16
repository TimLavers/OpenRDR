package io.rippledown.interpretation

import io.rippledown.constants.interpretation.*
import io.rippledown.main.Handler
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
    var conditionSelected: (selectedConditions: List<Condition>) -> Unit
    var onDone: (selectedConditions: List<Condition>) -> Unit
    var onCancel: () -> Unit
}

val ConditionSelector = FC<ConditionSelectorHandler> { handler ->
    var selected by useState(setOf<Condition>())

    fun handleChange(condition: Condition, checked: Boolean) {

        //This is important! If you don't clone the set, the state won't change and the checkbox won't update
        val selectedClone = selected.toMutableSet()
        if (checked) {
            selectedClone.add(condition)
        } else {
            selectedClone.remove(condition)
        }
        selected = selectedClone
        handler.conditionSelected(selectedClone.toList())
    }

    fun CheckboxControl(index: Int) = FC<CheckboxProps> { _ ->
        val condition = handler.conditions[index]
        Checkbox {
            id = "$CONDITION_SELECTOR_CHECKBOX$index"
            checked = selected.contains(condition)
            onChange = { _, isChecked ->
                handleChange(condition, isChecked)
            }
        }
    }.create()

    FormGroup {
        handler.conditions.forEachIndexed { index, condition ->
            FormControlLabel {
                id = "$CONDITION_SELECTOR_ROW$index"
                control = CheckboxControl(index)
                label = condition.asText().unsafeCast<ReactNode>()
            }
        }
    }

    FormHelperText {
        +"Select the reasons for making this change"
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
                handler.onDone(selected.toList())
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