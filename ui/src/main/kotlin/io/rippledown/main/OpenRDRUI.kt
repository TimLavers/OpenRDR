@file:OptIn(FlowPreview::class)

package io.rippledown.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.rippledown.appbar.AppBarHandler
import io.rippledown.appbar.ApplicationBar
import io.rippledown.casecontrol.*
import io.rippledown.interpretation.InterpretationActions
import io.rippledown.interpretation.InterpretationActionsHandler
import io.rippledown.model.Attribute
import io.rippledown.model.CasesInfo
import io.rippledown.model.KBInfo
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.edit.SuggestedCondition
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Diff
import io.rippledown.model.diff.Removal
import io.rippledown.model.rule.CornerstoneStatus
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
    var currentCase by remember { mutableStateOf<ViewableCase?>(null) }
    var currentCaseId by remember { mutableStateOf<Long?>(null) }
    var cornerstoneStatus: CornerstoneStatus? by remember { mutableStateOf(null) }
    var casesInfo by remember { mutableStateOf(CasesInfo()) }
    var kbInfo: KBInfo? by remember { mutableStateOf(null) }
    var infoMessage by remember { mutableStateOf("") }
    var conditionHints by remember { mutableStateOf(listOf<SuggestedCondition>()) }

    LaunchedEffect(Unit) {
        kbInfo = api.kbList().firstOrNull()
    }

    handler.setInfoMessage = {
        infoMessage = it
    }

    LaunchedEffect(casesInfo, currentCaseId) {
        if (casesInfo.caseIds.isNotEmpty()) {
            if (currentCaseId == null || currentCaseId !in casesInfo.caseIds.map { it.id }) {
                //No initial case, or it's now been deleted
                currentCaseId = casesInfo.caseIds[0].id!!
            }
            currentCase = runBlocking { handler.api.getCase(currentCaseId!!) }
            conditionHints = runBlocking { handler.api.conditionHints(currentCaseId!!).suggestions }
        }
    }
    val ruleInProgress = cornerstoneStatus != null

    Scaffold(
        topBar = {
            ApplicationBar(kbInfo, object : AppBarHandler {
                override var isRuleSessionInProgress = ruleInProgress
                override var selectKB: (id: String) -> Unit = { runBlocking { kbInfo = api.selectKB(it) } }
                override var createKB: (name: String) -> Unit = { runBlocking { kbInfo = api.createKB(it) } }
                override var createKBFromSample: (name: String, sample: SampleKB) -> Unit =
                    { name: String, sample: SampleKB ->
                        runBlocking {
                            kbInfo = api.createKBFromSample(name, sample)
                        }
                    }
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
            if (!ruleInProgress && currentCase != null) {
                val comments = currentCase!!.viewableInterpretation.conclusions().map { it.text }
                InterpretationActions(comments, object : InterpretationActionsHandler {
                    override fun startRuleToAddComment(comment: String) {
                        val sessionStartRequest = SessionStartRequest(
                            caseId = currentCase!!.id!!,
                            diff = Addition(comment)
                        )
                        cornerstoneStatus = runBlocking { api.startRuleSession(sessionStartRequest) }
                    }

                    override fun replaceComment() {
                        TODO("Not yet implemented")
                    }

                    override fun startRuleToRemoveComment(comment: String) {
                        val sessionStartRequest = SessionStartRequest(
                            caseId = currentCase!!.id!!,
                            diff = Removal(comment)
                        )
                        cornerstoneStatus = runBlocking { api.startRuleSession(sessionStartRequest) }
                    }
                })
            }
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
            Row {
                if (!ruleInProgress) {
                    handler.setInfoMessage("")
                    Column {
                        CaseSelectorHeader(casesInfo.caseIds.size)
                        Spacer(modifier = Modifier.height(10.dp))
                        CaseSelector(casesInfo.caseIds, object : CaseSelectorHandler, Handler by handler {
                            override var selectCase = { id: Long ->
                                currentCase = runBlocking { handler.api.getCase(id) }
                                currentCaseId = id
                            }
                        })
                    }
                }

                CaseControl(
                    currentCase = currentCase,
                    cornerstoneStatus = cornerstoneStatus,
                    conditionHints = conditionHints,
                    handler = object : CaseControlHandler, Handler by handler {
                        override fun endRuleSession() {
                            cornerstoneStatus = null
                        }

                        override fun onStartRule(selectedDiff: Diff) {}//todo remove
                        override fun buildRule(ruleRequest: RuleRequest) = runBlocking {
                            currentCase = api.buildRule(ruleRequest)
                            cornerstoneStatus = null
                        }

                        override fun updateCornerstoneStatus(cornerstoneRequest: UpdateCornerstoneRequest) =
                            runBlocking {
                                cornerstoneStatus = api.updateCornerstoneStatus(cornerstoneRequest)
                            }

                        override fun startRuleSession(sessionStartRequest: SessionStartRequest) = runBlocking {
                            cornerstoneStatus = api.startRuleSession(sessionStartRequest)
                        }

                        override var onInterpretationEdited: (text: String) -> Unit = { }
                        override var isCornerstone: Boolean = false
                        override fun getCase(caseId: Long) = runBlocking { currentCase = api.getCase(caseId) }

                        override fun saveCase(case: ViewableCase) = runBlocking {
                            currentCase = api.saveVerifiedInterpretation(case)
                        }

                        override var isClosing = { false }

                        override fun swapAttributes(moved: Attribute, target: Attribute) {
                            runBlocking {
                                api.moveAttribute(moved.id, target.id)
                            }
                        }

                        override suspend fun selectCornerstone(index: Int) = api.selectCornerstone(index)
                        override fun exemptCornerstone(index: Int) = runBlocking {
                            cornerstoneStatus = api.exemptCornerstone(index)
                        }
                    }
                )
            }
        }
    }
}
