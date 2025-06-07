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
import io.rippledown.constants.rule.RULE_MAKER
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.model.condition.edit.EditableCondition
import io.rippledown.model.condition.edit.NonEditableSuggestedCondition
import io.rippledown.model.condition.edit.SuggestedCondition
import kotlinx.coroutines.delay

interface RuleMakerHandler {
    var onDone: (conditions: List<Condition>) -> Unit
    var onCancel: () -> Unit
    var onUpdateConditions: (conditions: List<Condition>) -> Unit
    fun conditionForExpression(expression: String): ConditionParsingResult
}

const val DEBOUNCE: Long = 1_000

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RuleMaker(allConditions: List<SuggestedCondition>, handler: RuleMakerHandler) {
    var selectedConditions by remember { mutableStateOf(listOf<Condition>()) }
    var availableConditions by remember { mutableStateOf(listOf<SuggestedCondition>()) }
    var suggestionsUsed by remember { mutableStateOf(mapOf<Condition, SuggestedCondition>()) }
    var filterText by remember { mutableStateOf("") }
    var conditionToBeEdited by remember { mutableStateOf<EditableCondition?>(null) }
    var suggestionBeingEdited by remember { mutableStateOf<SuggestedCondition?>(null) }
    var showWaitingIndicator by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (suggestionBeingEdited != null) {
        val dialogState = rememberDialogState(size = DpSize(420.dp, 200.dp))
        DialogWindow(
            onCloseRequest = { suggestionBeingEdited = null },
            title = "Edit Condition",
            state = dialogState,
        ) {
            ConditionEditor(object : ConditionEditHandler {
                override fun editableCondition(): EditableCondition {
                    return conditionToBeEdited!!
                }

                override fun editingFinished(condition: Condition) {
                    // Remove the originating suggestion from the list of available suggestions.
                    if (suggestionBeingEdited!!.shouldBeUsedAtMostOncePerRule()) {
                        availableConditions = availableConditions - suggestionBeingEdited!!
                    }
                    // Associate the created condition with the suggestion
                    suggestionsUsed = suggestionsUsed + mapOf(condition to suggestionBeingEdited!!)
                    // Update the handler with the suggestions.
                    selectedConditions = selectedConditions + condition
                    handler.onUpdateConditions(selectedConditions)
                    // Clear the current editing suggestion, and hide the dialog.
                    clearAndHide()
                }

                override fun cancel() {
                    clearAndHide()
                }

                private fun clearAndHide() {
                    suggestionBeingEdited = null
                }
            })
        }
    }
    LaunchedEffect(filterText) {
        showWaitingIndicator = true
    }

    LaunchedEffect(allConditions, filterText) {
        showWaitingIndicator = true
        val conditionFor: (String) -> Condition? = { expression ->
            handler.conditionForExpression(expression).condition
        }
        availableConditions =
            refreshAvailableConditions(allConditions, filterText, selectedConditions, conditionFor)
        if (filterText.isNotBlank()) errorMessage = handler.conditionForExpression(filterText).errorMessage
        showWaitingIndicator = false
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
                if (correspondingSuggestion.shouldBeUsedAtMostOncePerRule()) {
                    availableConditions = availableConditions + correspondingSuggestion
                }
                handler.onUpdateConditions(selectedConditions)
            }
        })

        ConditionFilter(
            filterText,
            showWaitingIndicator,
            invalidExpression = errorMessage,
            handler = object : ConditionFilterHandler {
                override var onFilterChange = { filter: String ->
                    filterText = filter
                }
            })

        AvailableConditions(availableConditions, object : AvailableConditionsHandler {
            override fun onAddCondition(suggestedCondition: SuggestedCondition) {
                selectedConditions = selectedConditions + suggestedCondition.initialSuggestion()
                if (suggestedCondition.shouldBeUsedAtMostOncePerRule()) {
                    availableConditions = availableConditions - suggestedCondition
                }
                suggestionsUsed = suggestionsUsed + mapOf(suggestedCondition.initialSuggestion() to suggestedCondition)
                handler.onUpdateConditions(selectedConditions)
            }

            override fun onEditThenAdd(suggestedCondition: SuggestedCondition) {
                suggestionBeingEdited = suggestedCondition
                conditionToBeEdited = suggestedCondition.editableCondition()
                // Don't remove the suggestion from the available list yet.
                // We will remove it if and when editing results in a new condition being added.
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

internal suspend fun refreshAvailableConditions(
    allConditions: List<SuggestedCondition>,
    filterText: String,
    selectedConditions: List<Condition>,
    conditionFor: (String) -> Condition?,
): List<SuggestedCondition> {
    val filteredConditions = allConditions
        .sortedWith(compareBy { it.asText() })
        .filterConditions(filterText)
        .toMutableList()

    //If there is no matching condition, attempt to parse the filter text as a condition.
    if (filteredConditions.isEmpty()) {
        delay(DEBOUNCE)
        val parsed = conditionFor(filterText)
        if (parsed != null) {
            filteredConditions.add(0, NonEditableSuggestedCondition(parsed))
        }
    }

    // Remove conditions that are already selected.
    filteredConditions.removeAll { selectedConditions.contains(it.initialSuggestion()) }
    return filteredConditions
}

fun List<SuggestedCondition>.filterConditions(filter: String) = filter {
    it.asText().contains(filter, ignoreCase = true)
}
