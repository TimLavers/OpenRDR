@file:OptIn(FlowPreview::class)

package io.rippledown.main

import androidx.compose.material.BottomAppBar
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import io.rippledown.appbar.AppBarHandler
import io.rippledown.appbar.ApplicationBar
import io.rippledown.casecontrol.CaseControl
import io.rippledown.casecontrol.CaseControlHandler
import io.rippledown.casecontrol.CasePoller
import io.rippledown.casecontrol.CasePollerHandler
import io.rippledown.interpretation.Actions
import io.rippledown.model.Attribute
import io.rippledown.model.CasesInfo
import io.rippledown.model.KBInfo
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.diff.Diff
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
import io.rippledown.model.rule.UpdateCornerstoneRequest
import io.rippledown.sample.SampleKB
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import java.io.File

interface Handler {
    var api: Api
    var isClosing: () -> Boolean
    var setInfoMessage: (String) -> Unit
}

@Composable
fun OpenRDRUI(handler: Handler) {
    val api = handler.api
    var ruleInProgress by remember { mutableStateOf(false) }
    var casesInfo by remember { mutableStateOf(CasesInfo()) }
    var kbInfo: KBInfo? by remember { mutableStateOf(null) }
    var infoMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        kbInfo = api.kbList().firstOrNull()
    }

    handler.setInfoMessage = {
        infoMessage = it
    }

    Scaffold(
        topBar = {
            ApplicationBar(kbInfo, object : AppBarHandler {
                override var isRuleSessionInProgress = ruleInProgress
                override var selectKB: (id: String) -> Unit = { runBlocking { kbInfo = api.selectKB(it) } }
                override var createKB: (name: String) -> Unit = { runBlocking { kbInfo = api.createKB(it) } }
                override var createKBFromSample: (name: String, sample: SampleKB) -> Unit =
                    { name: String, sample: SampleKB -> runBlocking { kbInfo = api.createKBFromSample(name, sample) } }
                override var importKB: (data: File) -> Unit = { runBlocking { kbInfo = api.importKBFromZip(it) } }
                override var exportKB: (data: File) -> Unit = { runBlocking { api.exportKBToZip(it) } }
                override val kbList: () -> List<KBInfo> = { runBlocking { api.kbList() } }
            })
        },
        bottomBar = {
            BottomAppBar(
                backgroundColor = Color.White,
            )
            {
                InformationPanel(infoMessage)
            }
        },
        floatingActionButton = {
            Actions()
        }
    )
    {
        CasePoller(object : CasePollerHandler {
            override var onUpdate: (updated: CasesInfo) -> Unit = {
                casesInfo = it
            }
            override var updateCasesInfo: () -> CasesInfo = { runBlocking { api.waitingCasesInfo() } }
            override var isClosing: () -> Boolean = handler.isClosing
        })

        if (casesInfo.count > 0) {
            CaseControl(ruleInProgress, casesInfo, object : CaseControlHandler, Handler by handler {
                override var setRuleInProgress = { inProgress: Boolean ->
                    ruleInProgress = inProgress
                }

                override fun onStartRule(selectedDiff: Diff) {}//todo remove
                override fun buildRule(ruleRequest: RuleRequest) = runBlocking {
                    api.buildRule(ruleRequest)
                }

                override fun updateCornerstoneStatus(cornerstoneRequest: UpdateCornerstoneRequest) = runBlocking {
                    api.updateCornerstoneStatus(cornerstoneRequest)
                }

                override fun startRuleSession(sessionStartRequest: SessionStartRequest) = runBlocking {
                    api.startRuleSession(sessionStartRequest)
                }

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

                override suspend fun selectCornerstone(index: Int) = api.selectCornerstone(index)
                override fun exemptCornerstone(index: Int) = runBlocking { api.exemptCornerstone(index) }
            })
        }
    }
}
