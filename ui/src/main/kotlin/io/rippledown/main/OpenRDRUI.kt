package io.rippledown.main

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import io.rippledown.appbar.AppBarHandler
import io.rippledown.appbar.ApplicationBar
import io.rippledown.casecontrol.CaseControl
import io.rippledown.casecontrol.CaseControlHandler
import io.rippledown.casecontrol.CasePoller
import io.rippledown.casecontrol.CasePollerHandler
import io.rippledown.model.CasesInfo

interface Handler {
    var api: Api
    var isClosing: () -> Boolean
}

@Composable
@Preview
fun OpenRDRUI(handler: Handler) {
    var ruleInProgress by remember { mutableStateOf(false) }
    var casesInfo by remember { mutableStateOf(CasesInfo()) }

    Scaffold(
        topBar = {
            ApplicationBar(object : AppBarHandler, Handler by handler {
                override var isRuleSessionInProgress = ruleInProgress
            })
        }
    ) {
        CasePoller(object : CasePollerHandler, Handler by handler {
            override var updatedCasesInfo: (updated: CasesInfo) -> Unit = {
                casesInfo = it
            }
        })

        if (casesInfo.count > 0) {
            CaseControl(object : CaseControlHandler, Handler by handler {
                override var caseIds = casesInfo.caseIds
                override var setRuleInProgress = { inProgress: Boolean ->
                    ruleInProgress = inProgress
                }
            })
        }
    }
}
