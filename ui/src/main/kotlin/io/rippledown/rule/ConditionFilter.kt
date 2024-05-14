package io.rippledown.rule

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterVertically
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
fun ConditionFilter(filter: String, handler: ConditionFilterHandler) {
    Row(
        verticalAlignment = CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        OutlinedTextField(
            label = {
                Text(
                    text = "Select a condition for making this change",
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

    }

}