package io.rippledown.rule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.constants.interpretation.CONDITION_PREFIX
import io.rippledown.model.condition.Condition

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConditionToolTip(condition: Condition, content: @Composable () -> Unit) {
    TooltipArea(
        tooltip = {
            if (condition.userExpression().isNotBlank()) Text(
                text = condition.asText(),
                modifier = Modifier.padding(4.dp)
                    .semantics {
                        contentDescription = "$CONDITION_PREFIX${condition.asText()}"
                    }
            )
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

