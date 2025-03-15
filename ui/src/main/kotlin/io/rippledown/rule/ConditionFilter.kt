package io.rippledown.rule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
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
const val DOES_NOT_CORRESPOND_TO_A_CONDITION = "Does not correspond to a condition. Please try again."
const val ENTER_OR_SELECT_CONDITION = "Enter or select a condition for making this change"


@Composable
fun ConditionFilter(
    filter: String,
    showWaitingIndicator: Boolean,
    unknownExpression: Boolean = false,
    handler: ConditionFilterHandler
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            label = {
                val labelText = if (!unknownExpression) {
                    ENTER_OR_SELECT_CONDITION
                } else {
                    DOES_NOT_CORRESPOND_TO_A_CONDITION
                }
                Text(
                    text = labelText,
                    style = TextStyle(fontStyle = Italic),
                    //mergeDescendants = true is needed to make the label's contentDescription accessible via
                    //the Accessibility API, i.e. for cucumber tests
                    modifier = Modifier.semantics(mergeDescendants = true) {
                        contentDescription = labelText
                    }
                )
            },
            value = filter,
            onValueChange = {
                handler.onFilterChange(it)
            },
            trailingIcon = {
                if (unknownExpression) {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = "Error",
                        tint = Color(0xFFB00020)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        modifier = Modifier.padding(start = 20.dp)
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            isError = unknownExpression,
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
}



