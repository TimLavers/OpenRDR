package io.rippledown.rule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.constants.rule.RULE_MAKER
import io.rippledown.model.condition.Condition

interface RuleMakerHandler {
    var onDone: (conditions: List<Condition>) -> Unit
    var onCancel: () -> Unit
    var onUpdateConditions: (conditions: List<Condition>) -> Unit
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RuleMaker(allConditions: List<Condition>, handler: RuleMakerHandler) {
    var selectedConditions by remember { mutableStateOf(listOf<Condition>()) }
    var availableConditions by remember { mutableStateOf(listOf<Condition>()) }
    var filterText by remember { mutableStateOf("") }

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
                availableConditions = availableConditions + condition
                handler.onUpdateConditions(selectedConditions)
            }
        })

        ConditionFilter(filterText, object : ConditionFilterHandler {
            override var onFilterChange = { filter: String ->
                filterText = filter
                availableConditions = allConditions.filterConditions(filter) - selectedConditions
            }
        })

        AvailableConditions(availableConditions, object : AvailableConditionsHandler {
            override fun onAddCondition(condition: Condition) {
                selectedConditions = selectedConditions + condition
                availableConditions = availableConditions - condition
                handler.onUpdateConditions(selectedConditions)
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

fun List<Condition>.filterConditions(filter: String) = filter { it.asText().contains(filter, ignoreCase = true) }
