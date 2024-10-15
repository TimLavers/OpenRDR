package io.rippledown.rule

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle.Companion.Italic
import androidx.compose.ui.unit.dp
import io.rippledown.constants.rule.CURRENT_CONDITION

interface ConditionFilterHandler {
    var onFilterChange: (filter: String) -> Unit
}

@Composable
fun ConditionFilter(filter: String, showWaitingIndicator: Boolean, handler: ConditionFilterHandler) {
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
                Text(
                    text = "Enter a condition for making this change",
                    style = TextStyle(
                        fontStyle = Italic
                    )
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
            )
        }
    }

}



