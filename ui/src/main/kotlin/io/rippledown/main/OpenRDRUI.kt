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
import io.rippledown.model.Attribute
import io.rippledown.model.CasesInfo
import io.rippledown.model.KBInfo
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import java.io.File

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
                override var importKB: (data: File) -> Unit = { runBlocking { kbInfo = api.importKBFromZip(it) } }
                override var exportKB: (data: File) -> Unit = { runBlocking { api.exportKBToZip(it) } }
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
            CaseControl(false, casesInfo, object : CaseControlHandler, Handler by handler {
                override var setRuleInProgress = { inProgress: Boolean ->
                    ruleInProgress = inProgress
                }
                override var onStartRule: (indexOfSelectedDiff: Int) -> Unit = { }
                override var onInterpretationEdited: (text: String) -> Unit = { }
                override var isCornerstone: Boolean = false
                override var updateCase: (Long) -> Unit = { }
                override var caseEdited: () -> Unit = {}
                override var getCase: (caseId: Long) -> ViewableCase? = { runBlocking { api.getCase(it) } }
                override suspend fun saveCase(case: ViewableCase) = api.saveVerifiedInterpretation(case)
                override var isClosing = { false }
                override fun swapAttributes(moved: Attribute, target: Attribute) {
                    runBlocking {
                        api.moveAttribute(moved.id, target.id)
                    }
                }
                override suspend fun conditionHintsForCase(caseId: Long): List<Condition> {
                    return api.conditionHints(caseId).conditions
                }
            })
        }
    }
}
