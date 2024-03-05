package io.rippledown.main

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.rippledown.appbar.AppBarHandler
import io.rippledown.appbar.ApplicationBar
import io.rippledown.casecontrol.CasePoller
import io.rippledown.casecontrol.CasePollerHandler
import io.rippledown.constants.main.APPLICATION_BAR_ID
import io.rippledown.model.CasesInfo

interface Handler {
    var api: Api
}

@Composable
@Preview
fun OpenRDRUI(handler: Handler) {
    var ruleInProgress by remember { mutableStateOf(false) }
    var casesInfo by remember { mutableStateOf(CasesInfo()) }

    Scaffold(
        modifier = Modifier.testTag(APPLICATION_BAR_ID),
        topBar = {
            ApplicationBar( object : AppBarHandler, Handler by handler {
                override var isRuleSessionInProgress = ruleInProgress
            })
        }
    ) {
        CasePoller( object : CasePollerHandler, Handler by handler {
            override var isRuleSessionInProgress = ruleInProgress
            override var setRuleInProgress = { inProgress : Boolean ->
                ruleInProgress = inProgress
            }
            override var updatedCasesInfo: (updated: CasesInfo) -> Unit = {
                casesInfo = it
            }
        })
    }
}