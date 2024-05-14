package io.rippledown.rule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rippledown.model.condition.Condition

interface RuleMakerHandler {
    var onDone: (conditions: List<Condition>) -> Unit
    var onCancel: () -> Unit
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RuleMaker(allConditions: List<Condition>, handler: RuleMakerHandler) {
    var selectedConditions by remember { mutableStateOf(listOf<Condition>()) }
    var availableConditions by remember { mutableStateOf(allConditions) }
    var filterText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.heightIn(100.dp, 400.dp)
    ) {
        SelectedConditions(selectedConditions, object : SelectedConditionsHandler {
            override var onRemoveCondition = { condition: Condition ->
                selectedConditions = selectedConditions - condition
                availableConditions = availableConditions + condition
            }
        })

        ConditionFilter(filterText, object : ConditionFilterHandler {
            override var onFilterChange = { filter: String ->
                filterText = filter
                availableConditions = filterConditions(allConditions, filter) - selectedConditions
            }
        })

        AvailableConditions(availableConditions, object : AvailableConditionsHandler {
            override var onAddCondition = { condition: Condition ->
                selectedConditions = selectedConditions + condition
                availableConditions = availableConditions - condition
            }
        })
    }
}

fun filterConditions(conditions: List<Condition>, filter: String): List<Condition> {
    return conditions.filter { it.asText().contains(filter, ignoreCase = true) }
}
