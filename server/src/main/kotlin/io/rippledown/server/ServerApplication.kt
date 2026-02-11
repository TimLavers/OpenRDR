package io.rippledown.server

import io.rippledown.chat.Conversation
import io.rippledown.chat.ReasonTransformation
import io.rippledown.chat.ReasonTransformer
import io.rippledown.chat.toExpressionTransformation
import io.rippledown.constants.server.DEFAULT_PROJECT_NAME
import io.rippledown.kb.KB
import io.rippledown.kb.KBManager
import io.rippledown.kb.export.KBImporter
import io.rippledown.kb.export.util.Unzipper
import io.rippledown.kb.sample.loadSampleKB
import io.rippledown.log.lazyLogger
import io.rippledown.model.KBInfo
import io.rippledown.model.ServerChatResult
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.persistence.PersistenceProvider
import io.rippledown.persistence.postgres.PostgresPersistenceProvider
import io.rippledown.sample.SampleKB
import io.rippledown.server.chat.ChatManager
import io.rippledown.server.chat.KBChatService
import io.rippledown.server.chat.KbEditInterface
import io.rippledown.server.websocket.WebSocketManager
import io.rippledown.toJsonString
import io.rippledown.util.EntityRetrieval
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.io.path.createTempDirectory

class ServerApplication(
    private val persistenceProvider: PersistenceProvider = PostgresPersistenceProvider(),
    private val webSocketManager: WebSocketManager
) : ServerChatActionsInterface {
    private val logger = lazyLogger
    private val kbManager = KBManager(persistenceProvider, webSocketManager)
    private val idToKBEndpoint = mutableMapOf<String, KBEndpoint>()
    private lateinit var chatManager: ChatManager

    init {
        persistenceProvider.idStore().data().keys.forEach {
            val kbPersistence = persistenceProvider.kbPersistence(it)
            loadKnownKB(kbPersistence.kbInfo())
        }
        val chatService = ServerChatServiceFactory().createChatService()
        val conversation = Conversation(chatService = chatService, null)
        runBlocking{
            startConversation(null, null)
        }
//        chatManager = ChatManager(conversation, this)
    }

    fun getDefaultProject(): KBInfo {
        return kbManager.all().firstOrNull { it.name == DEFAULT_PROJECT_NAME } ?: createKB(DEFAULT_PROJECT_NAME, false)
    }

    fun createKB(name: String, force: Boolean): KBInfo {
        logger.info("Creating KB, name: $name, force: $force.")
        val kbInfo = kbManager.createKB(name, force)
        loadKnownKB(kbInfo)
        return kbInfo //todo test return value
    }

    fun createKBFromSample(name: String, sampleKB: SampleKB): KBInfo {
        logger.info("Creating Sample KB, name: $name, sample: $sampleKB.")
        val kbInfo = kbManager.createKB(name, false)
        loadKnownKB(kbInfo)
        loadSampleKB(kbFor(kbInfo), sampleKB)
        return kbInfo
    }

    fun selectKB(id: String): KBInfo {
        logger.info("Selecting kb with id: $id")
        val kbInfo = kbForId(id).kbInfo()
        loadKnownKB(kbInfo)
        runBlocking {
            webSocketManager.sendKbInfo(kbInfo)
        }
        logger.info("KB info sent to websocket, kb selected in server: $kbInfo.")
        return kbInfo
    }

    fun deleteKB(id: String) {
        TODO()
    }

    fun kbForId(id: String): KBEndpoint {
        return if (idToKBEndpoint.containsKey(id)) idToKBEndpoint[id]!! else throw IllegalArgumentException("Unknown kb id: $id")
    }

    fun kbForName(name: String) = kbInfoForName(name).map { kbForId(it.id) }

    private fun kbInfoForName(name: String): Result<KBInfo> {
        val searchName = name.lowercase().trim()
        val kbIdsForName = kbManager.all().filter { it.name.lowercase() == searchName }
        if (kbIdsForName.isEmpty()) {
            return Result.failure(IllegalArgumentException("No KB with name matching '$name' found."))
        }
        if (kbIdsForName.size > 1) {
            // Maybe there's an exact match.
            val exactMatches = kbManager.all().filter { it.name == name }
            if (exactMatches.size == 1) {
                return Result.success(exactMatches.first())
            }
            // No exact match, throw an error.
            val matches = kbIdsForName.map { it.name }.sorted().joinToString(", ")
            return Result.failure(IllegalArgumentException("More than one KB with name $name found."))
        }
        return Result.success(kbIdsForName.first())
    }

    fun kbFor(kbInfo: KBInfo) = kbForId(kbInfo.id)

    override fun kbList(): List<KBInfo> = kbManager.all().toList().sorted()

    fun importKBFromZip(zipBytes: ByteArray): KBInfo {
        val tempDir: File = createTempDirectory().toFile()
        Unzipper(zipBytes, tempDir).unzip()
        val subDirectories = tempDir.listFiles()
        require(subDirectories != null && subDirectories.size == 1) {
            "Invalid zip for KB import."
        }
        val rootDir = subDirectories[0]
        val kb = KBImporter(rootDir, persistenceProvider).import()
        logger.info("Imported KB with name: '${kb.kbInfo.name}' and id: '${kb.kbInfo.id}' from zip.")
        idToKBEndpoint[kb.kbInfo.id] = kbEndpoint(kb)
        return kb.kbInfo
    }

    suspend fun processUserRequest(request: String, kbId: String?): String {
        logger.info("processUserRequest, request: $request")
        return chatManager.response(request)
    }

    override fun openKB(name: String): Result<KBInfo> = runBlocking {
        val kbInfoResult = kbInfoForName(name)
        return@runBlocking if (kbInfoResult.isFailure) kbInfoResult else kbInfoResult.map { selectKB(it.id) }
    }

    override fun kb(id: String): KbEditInterface {
        TODO("Not yet implemented")
    }

    suspend fun startConversation(kbId: String?, caseId: Long?): String {
        val chatService = KBChatService.createKBChatService()
        if (kbId != null) {
            val kBEndpoint = kbForId(kbId)
            if (caseId != null) {
                val viewableCase = kBEndpoint.viewableCase(caseId)
                val conversationService = Conversation(
                    chatService = chatService,
                    reasonTransformer = createReasonTransformer(viewableCase, kBEndpoint.kb)
                )
                chatManager = ChatManager(conversationService, this)
                return chatManager.startConversation(kbId, viewableCase)
            } else {
                val conversationService = Conversation(
                    chatService = chatService,
                    reasonTransformer =  null
                )
                chatManager = ChatManager(conversationService, this)
                return chatManager.startConversation(kbId, null)
            }
        } else {
            val conversationService = Conversation(
                chatService = chatService,
                reasonTransformer =  null
            )
            chatManager = ChatManager(conversationService, this)
            return chatManager.startConversation(kbId, null)
        }
    }

    /**
     * Creates a transformer that converts a natural language reason into a rule condition and
     * adds it to the current rule session if it is valid.
     */
    fun createReasonTransformer(viewableCase: ViewableCase, ruleService: KbEditInterface) = object : ReasonTransformer {
        override suspend fun transform(reason: String): ReasonTransformation {
            val result = ruleService.conditionForExpression(viewableCase.case, reason)
            val condition = result.condition
            if (condition != null) {
                val cornerstoneStatus = ruleService.addConditionToCurrentRuleSession(condition)
                //inform the UI and the model of the update cornerstones
                //TODO TEST THIS
                chatManager.response(cornerstoneStatus.toJsonString())
            }
            return result.toExpressionTransformation()
        }
    }
    suspend fun responseToUserMessage(message: String) = chatManager.response(message)

    private fun kbEndpoint(kb: KB) = KBEndpoint(kb)

    private fun loadKnownKB(kbInfo: KBInfo) {
        val kb = (kbManager.openKB(kbInfo.id) as EntityRetrieval.Success<KB>).entity
        logger.info("Loaded KB with name: '${kbInfo.name}' and id: '${kbInfo.id}'.")
        idToKBEndpoint[kbInfo.id] = kbEndpoint(kb)
    }
}
