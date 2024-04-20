@file:OptIn(FlowPreview::class)

package io.rippledown.main

import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import io.rippledown.appbar.AppBarHandler
import io.rippledown.appbar.ApplicationBar
import io.rippledown.casecontrol.CaseControl
import io.rippledown.casecontrol.CaseControlHandler
import io.rippledown.casecontrol.CasePoller
import io.rippledown.casecontrol.CasePollerHandler
import io.rippledown.constants.interpretation.DEBOUNCE_WAIT_PERIOD_MILLIS
import io.rippledown.model.CasesInfo
import io.rippledown.model.KBInfo
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.diff.Diff
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking

interface Handler {
    var api: Api
    var isClosing: () -> Boolean
}

@Composable
fun OpenRDRUI(handler: Handler) {
    val api = handler.api
    var ruleInProgress by remember { mutableStateOf(false) }
    var casesInfo by remember { mutableStateOf(CasesInfo()) }
    var kbInfo: KBInfo? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        kbInfo = api.kbList().firstOrNull()
    }

    Scaffold(
        topBar = {
            ApplicationBar(kbInfo, object : AppBarHandler {
                override var isRuleSessionInProgress = ruleInProgress
                override var selectKB: (id: String) -> Unit = { runBlocking { kbInfo = api.selectKB(it) } }
                override var createKB: (name: String) -> Unit = { runBlocking { kbInfo = api.createKB(it) } }
                override val kbList: () -> List<KBInfo> = { runBlocking { api.kbList() } }
            })
        }
    ) {
        CasePoller(object : CasePollerHandler {
            override var onUpdate: (updated: CasesInfo) -> Unit = {
                casesInfo = it
            }
            override var updateCasesInfo: () -> CasesInfo = { runBlocking { api.waitingCasesInfo() } }
            override var isClosing: () -> Boolean = handler.isClosing
        })

        if (casesInfo.count > 0) {
            CaseControl(casesInfo, object : CaseControlHandler, Handler by handler {
                override var setRuleInProgress = { inProgress: Boolean ->
                    ruleInProgress = inProgress
                }
                override var onStartRule: (selectedDiff: Diff) -> Unit = { }
                override var onInterpretationEdited: (text: String) -> Unit = { }
                override var isCornerstone: Boolean = false
                override var updateCase: (Long) -> Unit = { }
                override var ruleSessionInProgress: (Boolean) -> Unit = { }
                override var caseEdited: () -> Unit = {}
                override var getCase: (caseId: Long) -> ViewableCase? = { runBlocking { api.getCase(it) } }
                override var saveCase: (case: ViewableCase) -> Unit = {
                    runBlocking {
                        flow {
                            emit(it)
                        }.debounce {
                            DEBOUNCE_WAIT_PERIOD_MILLIS
                        }.collect {
                            api.saveVerifiedInterpretation(it)
                        }
                    }
                }
                override var isClosing = { false }
            })
        }
    }
}
