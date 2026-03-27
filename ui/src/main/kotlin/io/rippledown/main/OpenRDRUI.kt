package io.rippledown.main

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.rippledown.appbar.AppBarHandler
import io.rippledown.appbar.ApplicationBar
import io.rippledown.casecontrol.*
import io.rippledown.chat.ChatController
import io.rippledown.chat.ChatControllerHandler
import io.rippledown.chat.VoiceRecognitionService
import io.rippledown.chat.VoiceRecognitionService.Companion.defaultModelPath
import io.rippledown.interpretation.toAnnotatedString
import io.rippledown.model.Attribute
import io.rippledown.model.CasesInfo
import io.rippledown.model.KBInfo
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse
import io.rippledown.model.diff.Diff
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.UndoRuleDescription
import io.rippledown.sample.SampleKB
import kotlinx.coroutines.*
import org.jetbrains.skiko.MainUIDispatcher
import java.awt.Cursor
import java.io.File

interface Handler {
    var api: Api
    var isClosing: () -> Boolean
    fun setWindowSize(isShowingCornerstone: Boolean)
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
    val voiceRecognitionService = remember { VoiceRecognitionService(defaultModelPath()) }
    var chatPanelWidth by remember { mutableStateOf(300.dp) }
    var conversationCaseId by remember { mutableStateOf<Long?>(null) }
    val density = LocalDensity.current

    val isShowingCornerstone = cornerstoneStatus?.cornerstoneToReview != null
    val ruleInProgress = cornerstoneStatus != null

    handler.setWindowSize(isShowingCornerstone)

    val chatControllerHandler = object : ChatControllerHandler {
        override var onBotMessageReceived: (response: ChatResponse) -> Unit = { }
        override fun sendUserMessage(message: String) {
            val caseId = requireNotNull(currentCaseId) {
                "currentCaseId should not be null when casesInfo.count > 0"
            }
            // Use dispatcher to ensure API calls run on the EDT
            CoroutineScope(dispatcher).launch {
                try {
                    if (conversationCaseId != caseId) {
                        api.startConversation(caseId)
                        conversationCaseId = caseId
                    }
                    val response = api.sendUserMessage(message, caseId)
                    onBotMessageReceived(response)

                    //refresh the case to get the latest interpretation
                    currentCase = api.getCase(caseId)
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
                if (currentCase?.case?.caseId?.id != currentCaseId) {
                    currentCase = api.getCase(currentCaseId!!)
                }
            }
        }
    }

    LaunchedEffect(currentCaseId) {
        withContext(dispatcher) {
            currentCaseId?.let {
                if (conversationCaseId != it) {
                    val response = api.startConversation(it)
                    conversationCaseId = it
                    if (response.text.isNotBlank()) {
                        chatControllerHandler.onBotMessageReceived(response)
                        ++chatId // Increment chatId to trigger recomposition in ChatController
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        withContext(dispatcher) {
            handler.api.startWebSocketSession(
                updateCornerstoneStatus = { cornerstoneStatus = it },
                ruleSessionCompleted = { cornerstoneStatus = null })
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
                override var lastRuleDescription: () -> UndoRuleDescription = { runBlocking { api.lastRuleDescription() } }
                override var undoLastRule: () -> Unit = {
                    runBlocking {
                        api.undoLastRule()
                        println("rule undone")
                        if (currentCaseId != null) {
                            println("refreshing case")
                            currentCase = api.getCase(currentCaseId!!)
                            println("case refreshed")
                        }
                    }
                }
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
            override var updateCasesInfo = {
                runBlocking(dispatcher) {
                    api.waitingCasesInfo()
                }
            }
            override var isClosing = handler.isClosing
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
                        handler = object : CaseControlHandler {
                            override var setRightInfoMessage: (message: String) -> Unit =
                                { rightInformationMessage = it }

                            override fun swapAttributes(moved: Attribute, target: Attribute) {
                                runBlocking(dispatcher) {
                                    api.moveAttribute(moved.id, target.id)
                                    currentCase = api.getCase(currentCaseId!!)
                                }
                            }

                            override fun selectCornerstone(index: Int) = runBlocking(dispatcher) {
                                cornerstoneStatus = api.selectCornerstone(index)
                            }

                            override fun exemptCornerstone(index: Int) = runBlocking(dispatcher) {
                                cornerstoneStatus = api.exemptCornerstone(index)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

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
                        voiceRecognitionService = voiceRecognitionService,
                        modifier = Modifier.width(chatPanelWidth)
                    )
                }
            }
        }
    }
}