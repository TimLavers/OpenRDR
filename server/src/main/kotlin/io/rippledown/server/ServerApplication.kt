package io.rippledown.server

import io.rippledown.constants.server.DEFAULT_PROJECT_NAME
import io.rippledown.kb.KB
import io.rippledown.kb.KBManager
import io.rippledown.kb.export.KBImporter
import io.rippledown.kb.export.util.Unzipper
import io.rippledown.kb.sample.loadSampleKB
import io.rippledown.log.lazyLogger
import io.rippledown.model.KBInfo
import io.rippledown.model.ServerChatResult
import io.rippledown.persistence.PersistenceProvider
import io.rippledown.persistence.postgres.PostgresPersistenceProvider
import io.rippledown.sample.SampleKB
import io.rippledown.server.websocket.WebSocketManager
import io.rippledown.util.EntityRetrieval
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.runBlocking

class ServerApplication(
    private val persistenceProvider: PersistenceProvider = PostgresPersistenceProvider(),
    private val webSocketManager: WebSocketManager
) {
    private val logger = lazyLogger

    //    val kbDataDir = File("data").apply { mkdirs() }
    private val kbManager = KBManager(persistenceProvider, webSocketManager)
    private val idToKBEndpoint = mutableMapOf<String, KBEndpoint>()
    private val chatManager: ServerChatManager

    init {
        persistenceProvider.idStore().data().keys.forEach {
            val kbPersistence = persistenceProvider.kbPersistence(it)
            loadKnownKB(kbPersistence.kbInfo())
        }
        val chatService = ServerChatServiceFactory().createChatService()
        chatManager = ServerChatManager(chatService, ChatActionsHandler(this))
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
        return kbForId(id).kbInfo()
    }

    fun deleteKB(id: String) {
        TODO()
    }

    fun kbForId(id: String): KBEndpoint {
        return if (idToKBEndpoint.containsKey(id)) idToKBEndpoint[id]!! else throw IllegalArgumentException("Unknown kb id: $id")
    }

    fun kbForName(name: String): Result<KBEndpoint> {
        val searchName = name.lowercase().trim()
        val kbIdsForName = kbManager.all().filter { it.name.lowercase() == searchName }
        if (kbIdsForName.isEmpty()) {
            return Result.failure(IllegalArgumentException("No KB with name matching '$name' found."))
        }
        if (kbIdsForName.size > 1) {
            // Maybe there's an exact match.
            val exactMatches = kbManager.all().filter { it.name == name }
            if (exactMatches.size == 1) {
                return Result.success(kbForId(exactMatches.first().id))
            }
            // No exact match, throw an error.
            val matches = kbIdsForName.map { it.name }.sorted().joinToString(", ")
            return Result.failure(IllegalArgumentException("These KBs matched '$name': $matches."))
        }
        return Result.success(kbForId(kbIdsForName.first().id))
    }

    fun kbFor(kbInfo: KBInfo) = kbForId(kbInfo.id)

    fun kbList(): List<KBInfo> = kbManager.all().toList().sorted()

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

    suspend fun processUserRequest(request: String, kbId: String?): ServerChatResult {
        logger.info("processUserRequest, request: $request")
        return chatManager.sendMessageAndActOnResponse(request, kbId)
    }

    class ChatActionsHandler(val serverApplication: ServerApplication) : ServerChatActionsInterface {
        override fun listKnowledgeBases() = serverApplication.kbList()
        override fun openKB(name: String) = serverApplication.kbForName(name).map { it.kbInfo() }
        override fun passUserMessageToKbChat(message: String, kbId: String) = runBlocking { serverApplication.kbForId(kbId).responseToUserMessage(message) }
    }

    private fun kbDataFile(kb: KB) = File(kbDataDir, kb.kbInfo.id)

    private fun kbEndpoint(kb: KB) = KBEndpoint(kb, kbDataFile(kb))
    private fun kbEndpoint(kb: KB) = KBEndpoint(kb)

    private fun loadKnownKB(kbInfo: KBInfo) {
        val kb = (kbManager.openKB(kbInfo.id) as EntityRetrieval.Success<KB>).entity
        logger.info("Loaded KB with name: '${kbInfo.name}' and id: '${kbInfo.id}'.")
        idToKBEndpoint[kbInfo.id] = kbEndpoint(kb)
    }
}
