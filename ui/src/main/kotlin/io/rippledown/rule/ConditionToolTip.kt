package io.rippledown.rule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import io.rippledown.model.condition.Condition

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConditionToolTip(condition: Condition, content: @Composable () -> Unit) {
    TooltipArea(
        tooltip = {
            if (condition.userExpression().isNotBlank()) Text(condition.asText())
        }
    ) {
        content()
    }
}

fun Condition.display() = if (userExpression().isNotBlank()) {
    userExpression()
} else {
    asText()
}

