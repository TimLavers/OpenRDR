package io.rippledown.rule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import io.rippledown.appbar.TextInputHandler
import io.rippledown.appbar.TextInputWithCancel
import io.rippledown.constants.main.CREATE
import io.rippledown.constants.main.CREATE_KB_NAME
import io.rippledown.constants.main.CREATE_KB_NAME_FIELD_DESCRIPTION
import io.rippledown.constants.main.CREATE_KB_OK_BUTTON_DESCRIPTION
import io.rippledown.constants.rule.RULE_MAKER
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.edit.EditableCondition
import io.rippledown.model.condition.edit.SuggestedCondition

interface RuleMakerHandler {
    var onDone: (conditions: List<Condition>) -> Unit
    var onCancel: () -> Unit
    var onUpdateConditions: (conditions: List<Condition>) -> Unit
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RuleMaker(allConditions: List<SuggestedCondition>, handler: RuleMakerHandler) {
    var selectedConditions by remember { mutableStateOf(listOf<Condition>()) }
    var availableConditions by remember { mutableStateOf(listOf<SuggestedCondition>()) }
    var suggestionsUsed by remember { mutableStateOf(mapOf<Condition, SuggestedCondition>()) }
    var filterText by remember { mutableStateOf("") }
    var editConditionDialogShowing by remember { mutableStateOf(false) }
    var conditionToBeEdited by remember { mutableStateOf<EditableCondition?>(null) }

    if (editConditionDialogShowing) {
        val dialogState = rememberDialogState(size = DpSize(420.dp, 160.dp))
        DialogWindow(
            onCloseRequest = { editConditionDialogShowing = false },
            title = "Edit Condition",
            state = dialogState,
        ) {
            ConditionEditor(object : ConditionEditHandler {
                override fun editableCondition(): EditableCondition {
                    return conditionToBeEdited!!
                }

                override fun editingFinished(condition: Condition) {
                    selectedConditions = selectedConditions + condition
                    editConditionDialogShowing = false
                }

                override fun cancel() {
                    editConditionDialogShowing = false
                }
            })
        }
    }

    LaunchedEffect(allConditions) {
        availableConditions = allConditions.sortedWith(compareBy { it.asText() })
    }
    Column(
        modifier = Modifier
            .requiredWidth(400.dp)
            .semantics { contentDescription = RULE_MAKER }
    ) {
        SelectedConditions(selectedConditions, object : SelectedConditionsHandler {
            override var onRemoveCondition = { condition: Condition ->
                selectedConditions = selectedConditions - condition
                val correspondingSuggestion = suggestionsUsed[condition]!!
                suggestionsUsed = suggestionsUsed - condition
                availableConditions = availableConditions + correspondingSuggestion
                handler.onUpdateConditions(selectedConditions)
            }
        })

        ConditionFilter(filterText, object : ConditionFilterHandler {
            override var onFilterChange = { filter: String ->
                filterText = filter
                availableConditions = allConditions.filterConditions(filter) - suggestionsUsed.values.toSet()
            }
        })

        AvailableConditions(availableConditions, object : AvailableConditionsHandler {
            override fun onAddCondition(suggestedCondition: SuggestedCondition) {
                selectedConditions = selectedConditions + suggestedCondition.initialSuggestion()
                availableConditions = availableConditions - suggestedCondition
                suggestionsUsed = suggestionsUsed + mapOf(suggestedCondition.initialSuggestion() to suggestedCondition)
                handler.onUpdateConditions(selectedConditions)
            }

            override fun onEditThenAdd(suggestedCondition: SuggestedCondition) {
                conditionToBeEdited = suggestedCondition.editableCondition()
                editConditionDialogShowing = true
                // Don't remove the suggestion from the available list yet.
                // We will remove it if and when editing results in a new condition being added.
//                availableConditions = availableConditions - suggestedCondition
            }
        })

        RuleControlButtons(object : RuleControlButtonsHandler {
            override var cancel = {
                handler.onCancel()
            }
            override var finish = {
                handler.onDone(selectedConditions)
            }
        })
    }
}

fun List<SuggestedCondition>.filterConditions(filter: String) = filter { it.asText().contains(filter, ignoreCase = true) }
