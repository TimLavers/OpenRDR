package io.rippledown.server

import io.rippledown.kb.KB
import io.rippledown.kb.KBManager
import io.rippledown.kb.KBSession
import io.rippledown.kb.chat.ChatCoordinator
import io.rippledown.kb.chat.ChatManagerFactory
import io.rippledown.kb.chat.KnowledgeBaseService
import io.rippledown.kb.export.KBImporter
import io.rippledown.kb.export.util.Unzipper
import io.rippledown.kb.sample.loadSampleKB
import io.rippledown.log.lazyLogger
import io.rippledown.model.KBInfo
import io.rippledown.model.UserId
import io.rippledown.persistence.PersistenceProvider
import io.rippledown.persistence.postgres.PostgresPersistenceProvider
import io.rippledown.sample.SampleKB
import io.rippledown.server.websocket.WebSocketManager
import io.rippledown.util.EntityRetrieval
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.createTempDirectory

class ServerApplication(
    private val persistenceProvider: PersistenceProvider = PostgresPersistenceProvider(),
    val webSocketManager: WebSocketManager
) {
    private val logger = lazyLogger

    private val kbManager = KBManager(persistenceProvider)
    private val idToKBEndpoint = ConcurrentHashMap<String, KBEndpoint>()

    // Users on different KBs do not contend, but they all share the set of KBs,
    // so its mutations are serialised here.
    private val kbSetLock = Any()
    val chatCoordinator = ChatCoordinator(ChatManagerFactory(::kbServiceFor), ::kbServiceFor)

    fun kbServiceFor(userId: UserId): KnowledgeBaseService =
        ApplicationKbService(this, webSocketManager, userId, chatCoordinator)

    init {
        persistenceProvider.idStore().data().keys.forEach {
            val kbPersistence = persistenceProvider.kbPersistence(it)
            loadKnownKB(kbPersistence.kbInfo())
        }
    }

    fun createKB(name: String, force: Boolean): KBInfo = synchronized(kbSetLock) {
        logger.info("Creating KB, name: $name, force: $force.")
        val kbInfo = kbManager.createKB(name, force)
        loadKnownKB(kbInfo)
        kbInfo
    }

    fun createKBFromSample(name: String, sampleKB: SampleKB): KBInfo = synchronized(kbSetLock) {
        logger.info("Creating Sample KB, name: $name, sample: $sampleKB.")
        val kbInfo = kbManager.createKB(name, false)
        loadKnownKB(kbInfo)
        loadSampleKB(kbFor(kbInfo), sampleKB)
        kbInfo
    }

    fun selectKB(id: String): KBInfo {
        logger.info("Selecting kb with id: $id")
        return kbForId(id).kbInfo()
    }

    fun deleteKB(id: String): KBInfo? = synchronized(kbSetLock) {
        val endpoint = kbForId(id)
        logger.info("Deleting KB with name: '${endpoint.kbInfo().name}' and id: '$id'.")
        val remaining = kbManager.deleteKB(endpoint.kbInfo())
        idToKBEndpoint.remove(id)
        remaining
    }

    fun renameKB(id: String, newName: String): KBInfo = synchronized(kbSetLock) {
        kbForId(id)
        kbManager.renameKB(id, newName)
    }

    fun openChatEndpoint(userId: UserId): KBEndpoint? = chatCoordinator.openEndpointFor(userId)

    fun kbForId(id: String): KBEndpoint {
        return idToKBEndpoint[id] ?: throw IllegalArgumentException("Unknown kb id: $id")
    }

    fun kbForName(name: String): KBEndpoint {
        val kbIdsForName = kbManager.all().filter { it.name == name }
        if (kbIdsForName.isEmpty()) {
            throw IllegalArgumentException("No KB with name $name found.")
        }
        if (kbIdsForName.size > 1) {
            throw IllegalArgumentException("More than one KB with name $name found.")
        }
        return kbForId(kbIdsForName.first().id)
    }

    fun kbFor(kbInfo: KBInfo) = kbForId(kbInfo.id)

    fun kbList(): List<KBInfo> = kbManager.all().toList().sorted()

    fun importKBFromZip(zipBytes: ByteArray): KBInfo = synchronized(kbSetLock) {
        val tempDir: File = createTempDirectory().toFile()
        Unzipper(zipBytes, tempDir).unzip()
        val subDirectories = tempDir.listFiles()
        require(subDirectories != null && subDirectories.size == 1) {
            "Invalid zip for KB import."
        }
        val rootDir = subDirectories[0]
        val kb = KBImporter(rootDir, persistenceProvider, kbManager::requireNameUnused).import()
        logger.info("Imported KB with name: '${kb.kbInfo.name}' and id: '${kb.kbInfo.id}' from zip.")
        kbManager.register(kb)
        idToKBEndpoint[kb.kbInfo.id] = kbEndpoint(kb)
        kb.kbInfo
    }

    private fun kbEndpoint(kb: KB) = KBEndpoint(KBSession(kb, webSocketManager))

    private fun loadKnownKB(kbInfo: KBInfo) {
        val kb = (kbManager.openKB(kbInfo.id) as EntityRetrieval.Success<KB>).entity
        logger.info("Loaded KB with name: '${kbInfo.name}' and id: '${kbInfo.id}'.")
        idToKBEndpoint[kbInfo.id] = kbEndpoint(kb)
    }
}
