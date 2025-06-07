package io.rippledown.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.rippledown.appbar.AppBarHandler
import io.rippledown.appbar.ApplicationBar
import io.rippledown.casecontrol.*
import io.rippledown.chat.ChatController
import io.rippledown.chat.ChatControllerHandler
import io.rippledown.interpretation.toAnnotatedString
import io.rippledown.model.Attribute
import io.rippledown.model.CasesInfo
import io.rippledown.model.KBInfo
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.edit.SuggestedCondition
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Diff
import io.rippledown.model.diff.Removal
import io.rippledown.model.diff.Replacement
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
import io.rippledown.model.rule.UpdateCornerstoneRequest
import io.rippledown.sample.SampleKB
import kotlinx.coroutines.*
import org.jetbrains.skiko.MainUIDispatcher
import java.io.File

interface Handler {
    var api: Api
    var isClosing: () -> Boolean
    fun showingCornerstone(isShowingCornerstone: Boolean)
}

@Composable
fun OpenRDRUI(handler: Handler, dispatcher: CoroutineDispatcher = MainUIDispatcher) {
    val api = handler.api
    var currentCase by remember { mutableStateOf<ViewableCase?>(null) }
    var currentCaseId by remember { mutableStateOf<Long?>(null) }
    var chatId by remember { mutableStateOf<Long>(-1) }
    var cornerstoneStatus: CornerstoneStatus? by remember { mutableStateOf(null) }
    var casesInfo by remember { mutableStateOf(CasesInfo()) }
    var kbInfo: KBInfo? by remember { mutableStateOf(null) }
    var rightInformationMessage by remember { mutableStateOf("") }
    var ruleAction: Diff? by remember { mutableStateOf(null) }
    var conditionHints by remember { mutableStateOf(listOf<SuggestedCondition>()) }
    var isChatVisible by remember { mutableStateOf(false) }
    var isChatEnabled by remember { mutableStateOf(true) }

    val isShowingCornerstone = cornerstoneStatus?.cornerstoneToReview != null
    val ruleInProgress = cornerstoneStatus != null
    handler.showingCornerstone(isShowingCornerstone)

    val chatControllerHandler = object : ChatControllerHandler {
        override var onBotMessageReceived: (message: String) -> Unit = { }
        override fun sendUserMessage(message: String) {
            val caseId = requireNotNull(currentCaseId) {
                "currentCaseId should not be null when casesInfo.count > 0"
            }
            // Use dispatcher to ensure API calls run on the EDT
            CoroutineScope(dispatcher).launch {
                val response = api.sendUserMessage(message, caseId)
                onBotMessageReceived(response)

                //refresh the case to get the latest interpretation
                currentCase = api.getCase(caseId)
                ++chatId // Increment chatId to trigger recomposition in ChatController
            }
        }
    }


    LaunchedEffect(Unit) {
        withContext(dispatcher) {
            kbInfo = api.kbList().firstOrNull()
        }
    }

    LaunchedEffect(casesInfo, currentCaseId) {
        withContext(dispatcher) {
            if (casesInfo.caseIds.isNotEmpty()) {
                if (currentCaseId == null || currentCaseId !in casesInfo.caseIds.map { it.id }) {
                    // No initial case, or it's now been deleted
                    currentCaseId = casesInfo.caseIds[0].id!!
                }
                currentCase = api.getCase(currentCaseId!!)
                conditionHints = api.conditionHints(currentCaseId!!).suggestions
                isChatEnabled = true
            }
        }
    }

    LaunchedEffect(currentCaseId) {
        // When currentCaseId changes, start a conversation with the model if the chat panel is visible
        if (isChatVisible) {
            withContext(dispatcher) {
                currentCaseId?.let {
                    val response = api.startConversation(it)
                    if (response.isNotBlank()) {
                        chatControllerHandler.onBotMessageReceived(response)
                        ++chatId // Increment chatId to trigger recomposition in ChatController
                    }
                }
            }
        }
    }

    //Start a conversation with the model when the chat is made visible
    LaunchedEffect(isChatVisible) {
        withContext(dispatcher) {
            currentCaseId?.let {
                val response = api.startConversation(it)
                if (response.isNotBlank()) {
                    chatControllerHandler.onBotMessageReceived(response)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            ApplicationBar(kbInfo, isChatVisible, isChatEnabled, object : AppBarHandler {
                override var isRuleSessionInProgress = ruleInProgress
                override var selectKB: (id: String) -> Unit = {
                    CoroutineScope(dispatcher).launch {
                        kbInfo = api.selectKB(it)
                    }
                }
                override var createKB: (name: String) -> Unit = {
                    CoroutineScope(dispatcher).launch {
                        kbInfo = api.createKB(it)
                    }
                }
                override var createKBFromSample: (name: String, sample: SampleKB) -> Unit =
                    { name: String, sample: SampleKB ->
                        CoroutineScope(dispatcher).launch {
                            kbInfo = api.createKBFromSample(name, sample)
                        }
                    }
                override var importKB: (data: File) -> Unit = {
                    CoroutineScope(dispatcher).launch {
                        kbInfo = api.importKBFromZip(it)
                    }
                }
                override var exportKB: (data: File) -> Unit = {
                    CoroutineScope(dispatcher).launch {
                        api.exportKBToZip(it)
                    }
                }
                override val kbList: () -> List<KBInfo> = {
                    runBlocking(dispatcher) { api.kbList() }
                }
                override var setKbDescription: (description: String) -> Unit = {
                    CoroutineScope(dispatcher).launch {
                        api.setKbDescription(it)
                    }
                }
                override var kbDescription: () -> String = {
                    runBlocking(dispatcher) { api.kbDescription() }
                }
                override var onToggleChat: () -> Unit = { isChatVisible = !isChatVisible }
            })
        },
        bottomBar = {
            BottomAppBar(
                backgroundColor = Color.White,
            ) {
                val leftMessage = ruleAction?.toAnnotatedString() ?: AnnotatedString("")
                val rightMessage = AnnotatedString(rightInformationMessage)
                InformationPanel(leftMessage, rightMessage)
            }
        },
    ) { paddingValues ->
        CasePoller(object : CasePollerHandler {
            override var onUpdate: (updated: CasesInfo) -> Unit = {
                casesInfo = it
            }
            override var updateCasesInfo: () -> CasesInfo = {
                val r = runBlocking(dispatcher) {
                    api.waitingCasesInfo()
                }
                r
            }
            override var isClosing: () -> Boolean = handler.isClosing
        }, dispatcher)

        if (casesInfo.count > 0) {
            Column(modifier = Modifier.padding(paddingValues)) {
                Row(modifier = Modifier.weight(1f)) {
                    if (!ruleInProgress) {
                        ruleAction = null
                        rightInformationMessage = ""
                        Column {
                            CaseSelectorHeader(casesInfo.caseIds.size)
                            Spacer(modifier = Modifier.height(8.dp))
                            CaseSelector(casesInfo.caseIds, object : CaseSelectorHandler, Handler by handler {
                                override var selectCase = { id: Long ->
                                    ++chatId
                                    currentCase = runBlocking(dispatcher) { api.getCase(id) }
                                    currentCaseId = id
                                }
                            })
                        }
                    }

                    CaseControl(
                        currentCase = currentCase,
                        cornerstoneStatus = cornerstoneStatus,
                        conditionHints = conditionHints,
                        handler = object : CaseControlHandler {
                            override fun allComments() =
                                runBlocking(dispatcher) { api.allConclusions().map { it.text }.toSet() }

                            override fun startRuleToAddComment(comment: String) {
                                ruleAction = Addition(comment)
                                val sessionStartRequest = SessionStartRequest(
                                    caseId = currentCase!!.id!!,
                                    diff = ruleAction as Addition
                                )
                                CoroutineScope(dispatcher).launch {
                                    cornerstoneStatus = api.startRuleSession(sessionStartRequest)
                                }
                            }

                            override fun startRuleToReplaceComment(toBeReplaced: String, replacement: String) {
                                ruleAction = Replacement(toBeReplaced, replacement)
                                val sessionStartRequest = SessionStartRequest(
                                    caseId = currentCase!!.id!!,
                                    diff = ruleAction as Replacement
                                )
                                CoroutineScope(dispatcher).launch {
                                    cornerstoneStatus = api.startRuleSession(sessionStartRequest)
                                }
                            }

                            override fun startRuleToRemoveComment(comment: String) {
                                ruleAction = Removal(comment)
                                val sessionStartRequest = SessionStartRequest(
                                    caseId = currentCase!!.id!!,
                                    diff = ruleAction as Removal
                                )
                                CoroutineScope(dispatcher).launch {
                                    cornerstoneStatus = api.startRuleSession(sessionStartRequest)
                                }
                            }

                            override fun endRuleSession() {
                                CoroutineScope(dispatcher).launch {
                                    api.cancelRuleSession()
                                    cornerstoneStatus = null
                                }
                            }

                            override var setRightInfoMessage: (message: String) -> Unit =
                                { rightInformationMessage = it }

                            override fun buildRule(ruleRequest: RuleRequest) = runBlocking(dispatcher) {
                                currentCase = api.commitSession(ruleRequest)
                                cornerstoneStatus = null
                            }

                            override fun updateCornerstoneStatus(cornerstoneRequest: UpdateCornerstoneRequest) =
                                runBlocking(dispatcher) {
                                    cornerstoneStatus = api.updateCornerstoneStatus(cornerstoneRequest)
                                }

                            override fun startRuleSession(sessionStartRequest: SessionStartRequest) =
                                runBlocking(dispatcher) {
                                    cornerstoneStatus = api.startRuleSession(sessionStartRequest)
                                }

                            override fun getCase(caseId: Long) =
                                runBlocking(dispatcher) { currentCase = api.getCase(caseId) }

                            override fun swapAttributes(moved: Attribute, target: Attribute) {
                                runBlocking(dispatcher) {
                                    api.moveAttribute(moved.id, target.id)
                                }
                            }

                            override fun selectCornerstone(index: Int) = runBlocking(dispatcher) {
                                cornerstoneStatus = api.selectCornerstone(index)
                            }

                            override fun exemptCornerstone(index: Int) = runBlocking(dispatcher) {
                                cornerstoneStatus = api.exemptCornerstone(index)
                            }

                            override fun conditionFor(
                                conditionText: String,
                            ) = runBlocking(dispatcher) {
                                api.conditionFor(conditionText)
                            }
                        },
                        modifier = if (isChatVisible) {
                            Modifier.weight(0.7f)
                        } else {
                            Modifier.fillMaxSize()
                        }
                    )

                    if (isChatVisible) {
                        ChatController(
                            id = chatId,
                            chatControllerHandler,
                            modifier = Modifier.weight(0.3f)
                        )
                    }
                }
            }
        }
    }
}