package io.rippledown.rule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle.Companion.Italic
import androidx.compose.ui.unit.dp
import io.rippledown.constants.rule.CURRENT_CONDITION

interface ConditionFilterHandler {
    var onFilterChange: (filter: String) -> Unit
}

const val WAITING_INDICATOR = "WAITING_INDICATOR"
const val DOES_NOT_CORRESPOND_TO_A_CONDITION = "This condition is not able to be recognised. Please try again."
const val CONDITION_IS_NOT_TRUE = "This condition is not true for this case. Please try again."
const val ENTER_OR_SELECT_CONDITION = "Enter or select a condition for making this change"

@Composable
fun ConditionFilter(
    filter: String,
    showWaitingIndicator: Boolean,
    invalidExpression: String? = null,
    handler: ConditionFilterHandler
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                label = {
                    Text(
                        text = ENTER_OR_SELECT_CONDITION,
                        style = TextStyle(fontStyle = Italic),
                        //mergeDescendants = true is needed to make the label's contentDescription accessible via
                        //the Accessibility API, i.e. for cucumber tests
                        modifier = Modifier.semantics(mergeDescendants = true) {
                            contentDescription = ENTER_OR_SELECT_CONDITION
                        }
                    )
                },
                value = filter,
                onValueChange = {
                    handler.onFilterChange(it)
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        modifier = Modifier.padding(start = 20.dp)
                    )
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .semantics {
                        contentDescription = CURRENT_CONDITION
                    }
            )
            Spacer(modifier = Modifier.width(10.dp))

            if (showWaitingIndicator) {
                CircularProgressIndicator(
                    strokeWidth = 1.dp,
                    modifier = Modifier
                        .size(25.dp)
                        .semantics {
                            contentDescription = WAITING_INDICATOR
                        }
                )
            }
        }
        if (invalidExpression != null) {
            Text(
                text = invalidExpression,
                style = TextStyle(fontStyle = Italic),
                color = Color(0xFFB00020),
                modifier = Modifier.padding(top = 5.dp, start = 10.dp)
                    .semantics {
                        contentDescription = invalidExpression
                    }
            )
        }
    }
}



