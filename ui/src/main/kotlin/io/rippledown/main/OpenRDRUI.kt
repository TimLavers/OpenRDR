package io.rippledown.main

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.rippledown.appbar.AppBarHandler
import io.rippledown.appbar.ApplicationBar
import io.rippledown.casecontrol.CaseControl
import io.rippledown.casecontrol.CaseControlHandler
import io.rippledown.casecontrol.CaseSelector
import io.rippledown.casecontrol.CaseSelectorHandler
import io.rippledown.chat.ChatController
import io.rippledown.chat.ChatControllerHandler
import io.rippledown.cornerstone.CornerstoneTestHook
import io.rippledown.model.Attribute
import io.rippledown.model.CasesInfo
import io.rippledown.model.KBInfo
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse
import io.rippledown.model.report.CaseReport
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.sample.SampleKB
import io.rippledown.voice.VoiceRecognition
import io.rippledown.voice.VoiceRecognitionService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skiko.MainUIDispatcher
import java.awt.Cursor
import java.io.File

interface Handler {
    var api: Api
    var isClosing: () -> Boolean
    fun setWindowSize(isShowingCornerstone: Boolean)
}

@Composable
fun OpenRDRUI(
    handler: Handler,
    dispatcher: CoroutineDispatcher = MainUIDispatcher,
    voiceRecognition: VoiceRecognition? = null
) {
    val api = handler.api
    // An Attribute is equal to another with the same id, whatever its name (see
    // Attribute.equals), so a case whose only change is a renamed attribute is
    // structurally equal to the case it replaces. Under the default state policy
    // that refresh would be discarded and the old name would stay on screen, so
    // a refreshed case is always treated as a new value.
    var currentCase by remember { mutableStateOf<ViewableCase?>(null, neverEqualPolicy()) }
    var currentCaseId by remember { mutableStateOf<Long?>(null) }
    var chatId by remember { mutableStateOf<Long>(-1) }
    var cornerstoneStatus: CornerstoneStatus? by remember { mutableStateOf(null) }
    var casesInfo by remember { mutableStateOf(CasesInfo()) }
    // KBInfo equality is id-only, so a rename produces an equal value. Always
    // accept pushed KBInfo values so the application bar observes name changes.
    var kbInfo: KBInfo? by remember { mutableStateOf(null, neverEqualPolicy()) }
    val voiceRecognitionService = voiceRecognition ?: remember { VoiceRecognitionService() }
    var chatPanelWidth by remember { mutableStateOf(350.dp) }
    var pendingConversationResponse by remember { mutableStateOf<ChatResponse?>(null) }
    // What the chat knows about is settled only once the KB list has been read
    // and, if a KB is open, its cases fetched; until then no conversation is
    // started, or the user would see a greeting for a state that is passing.
    var kbListRead by remember { mutableStateOf(false) }
    var casesInfoKbId by remember { mutableStateOf<String?>(null) }
    var conversationStarted by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    // Report panel state
    var reportVisible by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<CaseReport?>(null) }
    var isLoadingReport by remember { mutableStateOf(false) }

    // Create CaseSelectorHandler reference
    val caseSelectorHandler = remember {
        object : CaseSelectorHandler {
            override var selectCase: (id: Long) -> Unit = { }
            override var requestFocusOnSelectedCase: () -> Unit = { }
        }
    }

    val isShowingCornerstone = cornerstoneStatus?.cornerstoneToReview != null
    val ruleInProgress = cornerstoneStatus != null

    // Publish the cornerstone state to [CornerstoneTestHook] so in-JVM
    // integration tests (cucumber) can poll it without walking the
    // accessibility tree. Same rationale as [ChatTestHook]; see those
    // class docs for why the accessibility bridge is unusable on a
    // window containing a large case table.
    SideEffect {
        CornerstoneTestHook.update(cornerstoneStatus)
    }

    handler.setWindowSize(isShowingCornerstone)

    val chatControllerHandler = object : ChatControllerHandler {
        override var onBotMessageReceived: (response: ChatResponse) -> Unit = { }
        override fun sendUserMessage(message: String) {
            // Use dispatcher to ensure API calls run on the EDT
            CoroutineScope(dispatcher).launch {
                try {
                    val response = api.sendUserMessage(message)
                    onBotMessageReceived(response)

                    //refresh the case to get the latest interpretation
                    currentCaseId?.let { currentCase = api.getCase(it) }
                    ++chatId // Increment chatId to trigger recomposition in ChatController
                } catch (_: Exception) {
                    //ignore
                    //a test may shut down the server before this message can be sent
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        withContext(dispatcher) {
            // Pick the first KB and explicitly select it on the server so
            // that Api.currentKB matches what the UI displays. Just reading
            // kbList() leaves Api.currentKB unset, which would later cause
            // the lazy `kbInfo()` path to fetch the default KB and route
            // subsequent requests to the wrong KB.
            kbInfo = api.kbList().firstOrNull()?.let { api.selectKB(it.id) }
            kbListRead = true
        }
    }

    LaunchedEffect(kbInfo) {
        withContext(dispatcher) {
            val open = kbInfo
            if (open?.id != casesInfoKbId) {
                // Case ids are per KB, so a case of the new KB can share the id of the
                // one showing; drop it so the new KB's case is fetched.
                currentCaseId = null
                currentCase = null
                cornerstoneStatus = null
            }
            casesInfo = if (open == null) CasesInfo() else api.waitingCasesInfo()
            casesInfoKbId = open?.id
        }
    }

    // Tracks the ordering of all known case ids (Processed + Cornerstone +
    // Favourite), as last reported by the server, so that if the currently
    // selected case is later removed from that combined list (e.g. it was
    // deleted), we can work out which case was showing immediately before it
    // and re-select that one instead of always falling back to the first case.
    var lastKnownCaseOrder by remember { mutableStateOf<List<Long>>(emptyList()) }

    LaunchedEffect(casesInfo, currentCaseId) {
        withContext(dispatcher) {
            val allCaseIds = casesInfo.caseIds + casesInfo.cornerstoneCaseIds + casesInfo.favouriteCaseIds
            val allIdValues = allCaseIds.mapNotNull { it.id }
            if (allIdValues.isNotEmpty()) {
                if (currentCaseId == null) {
                    // No initial case.
                    currentCaseId = casesInfo.caseIds.firstOrNull()?.id ?: allIdValues[0]
                } else if (currentCaseId !in allIdValues) {
                    // The current case is no longer represented in the ids received
                    // from the server, e.g. it's been deleted. Select the case that
                    // was showing immediately before it, if there is one, else fall
                    // back to the first case in the Processed list.
                    val previousIndex = lastKnownCaseOrder.indexOf(currentCaseId)
                    val previousCaseId = if (previousIndex > 0) lastKnownCaseOrder[previousIndex - 1] else null
                    currentCaseId = if (previousCaseId != null && previousCaseId in allIdValues) {
                        previousCaseId
                    } else {
                        casesInfo.caseIds.firstOrNull()?.id ?: allIdValues[0]
                    }
                }
                if (currentCase?.case?.caseId?.id != currentCaseId) {
                    currentCase = api.getCase(currentCaseId!!)
                }
            }
            lastKnownCaseOrder = allIdValues
        }
    }

    // The context the chat is about: no KB, a KB with no cases, or a case. Null
    // while the state is in flux (KB list not yet read, cases not yet fetched for
    // the open KB, or a case about to be chosen), so that a conversation is started
    // once per settled context and not for the states passed through on the way.
    val chatContext: Pair<String?, Long?>? = run {
        val open = kbInfo
        val caseId = currentCaseId
        when {
            !kbListRead -> null
            open == null -> Pair(null, null)
            casesInfoKbId != open.id -> null
            casesInfo.count == 0 -> Pair(open.id, null)
            caseId == null -> null
            (casesInfo.caseIds + casesInfo.cornerstoneCaseIds + casesInfo.favouriteCaseIds).none { it.id == caseId } -> null
            else -> Pair(open.id, caseId)
        }
    }

    LaunchedEffect(chatContext) {
        val (kbId, caseId) = chatContext ?: return@LaunchedEffect
        conversationStarted = false
        withContext(dispatcher) {
            try {
                val response = api.startConversation(kbId, caseId)
                ++chatId
                if (response.text.isNotBlank()) {
                    pendingConversationResponse = response
                }
            } catch (_: Exception) {
                // Swallow transient failures (e.g. stale kb id during a kb switch,
                // or a case that is not (yet) in the current kb). The effect will
                // re-fire when the context changes again.
            } finally {
                conversationStarted = true
            }
        }
    }

    // Clear any stale report as soon as the selected case changes, so the
    // previous case's report is not shown while the new one is generated.
    LaunchedEffect(currentCaseId) {
        report = null
    }

    // Generate the report when the panel is visible and no rule session is
    // active. Keyed on the case id and its comment text (not the whole
    // ViewableCase) so it regenerates when the comments change but not on every
    // unrelated case refresh (e.g. after each chat message).
    val reportComments = currentCase?.viewableInterpretation?.latestText()
    LaunchedEffect(reportVisible, currentCaseId, reportComments, ruleInProgress) {
        withContext(dispatcher) {
            val caseId = currentCaseId
            if (reportVisible && caseId != null && !ruleInProgress) {
                isLoadingReport = true
                report = api.getCaseReport(caseId)
                isLoadingReport = false
            }
        }
    }

    LaunchedEffect(Unit) {
        withContext(dispatcher) {
            handler.api.startWebSocketSession(
                updateCornerstoneStatus = {
                    cornerstoneStatus = it
                },
                ruleSessionCompleted = {
                    cornerstoneStatus = null
                },
                updateCasesInfo = { incoming ->
                    // Ignore updates that belong to a different KB than the one
                    // the UI is currently showing. Otherwise a sample-KB build
                    // on the server can push casesInfo for the new KB before
                    // the UI has finished switching, leaving casesInfo and
                    // Api.currentKB out of sync (causing 500s on follow-up
                    // calls like startConversation).
                    val current = kbInfo?.name
                    if (current == null || incoming.kbName.isBlank() || incoming.kbName == current) {
                        casesInfo = incoming
                    }
                },
                kbInfoUpdated = { kbInfo = it },
                kbClosed = { kbInfo = null }
            )
        }
    }

    Scaffold(
        topBar = {
            ApplicationBar(kbInfo, object : AppBarHandler {
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
                override suspend fun kbList(): List<KBInfo> = api.kbList()
                override var setKbDescription: (description: String) -> Unit = {
                    CoroutineScope(dispatcher).launch {
                        api.setKbDescription(it)
                    }
                }
                override suspend fun kbDescription(): String = api.kbDescription()
            })
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Row(modifier = Modifier.weight(1f)) {
                if (casesInfo.count > 0 && !ruleInProgress) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        CaseSelector(
                            casesInfo.caseIds,
                            casesInfo.cornerstoneCaseIds,
                            caseSelectorHandler,
                            casesInfo.favouriteCaseIds,
                            currentCaseId
                        )
                    }

                    // Set the selectCase callback after caseSelectorHandler is created
                    caseSelectorHandler.selectCase = { id: Long ->
                        CoroutineScope(dispatcher).launch {
                            currentCase = api.getCase(id)
                            currentCaseId = id
                        }
                    }
                }

                if (casesInfo.count > 0) {
                    CaseControl(
                        currentCase = currentCase,
                        cornerstoneStatus = cornerstoneStatus,
                        handler = object : CaseControlHandler {
                            override fun swapAttributes(moved: Attribute, target: Attribute) {
                                CoroutineScope(dispatcher).launch {
                                    api.moveAttribute(moved.id, target.id)
                                    currentCase = api.getCase(currentCaseId!!)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        reportVisible = reportVisible,
                        reportText = report?.markdown,
                        reportGenerated = report?.generated ?: true,
                        isLoadingReport = isLoadingReport,
                        onReportToggle = { reportVisible = it }
                    )
                } else {
                    // Keep the chat panel on the right, where it sits once there are cases.
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Draggable divider for resizing the chat panel
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(Color.LightGray)
                        .pointerHoverIcon(PointerIcon(Cursor(Cursor.W_RESIZE_CURSOR)))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val deltaWidth = with(density) { (-dragAmount.x).toDp() }
                                chatPanelWidth = (chatPanelWidth + deltaWidth).coerceIn(200.dp, 600.dp)
                            }
                        }
                )

                ChatController(
                    id = chatId,
                    chatControllerHandler,
                    conversationStarted = conversationStarted,
                    voiceRecognitionService = voiceRecognitionService,
                    modifier = Modifier.width(chatPanelWidth)
                )
            }
            LaunchedEffect(pendingConversationResponse) {
                pendingConversationResponse?.let {
                    chatControllerHandler.onBotMessageReceived(it)
                    pendingConversationResponse = null
                }
            }
        }
    }
}