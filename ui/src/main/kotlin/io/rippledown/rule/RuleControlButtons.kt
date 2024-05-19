@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)

package io.rippledown.rule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.constants.rule.CANCEL_RULE_BUTTON
import io.rippledown.constants.rule.FINISH_RULE_BUTTON

interface RuleControlButtonsHandler {
    var cancel: () -> Unit
    var finish: () -> Unit
}

@Composable
fun RuleControlButtons(handler: RuleControlButtonsHandler) {
    Row(
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        Button(
            onClick = {
                handler.cancel
            },
            modifier = Modifier.semantics { contentDescription = CANCEL_RULE_BUTTON }
        ) {
            Text("Cancel rule")
        }
        Button(
            onClick = {
                handler.finish()
            },
            modifier = Modifier.semantics { contentDescription = FINISH_RULE_BUTTON }
        ) {
            Text("Finish rule")
        }
    }

}
