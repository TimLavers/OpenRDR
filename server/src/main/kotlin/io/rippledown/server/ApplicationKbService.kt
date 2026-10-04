package io.rippledown.server

import io.rippledown.kb.KbResolution
import io.rippledown.kb.chat.KnowledgeBaseService
import io.rippledown.kb.chat.OpenKnowledgeBases
import io.rippledown.kb.isDemonstrationTitle
import io.rippledown.kb.nearDuplicateOf
import io.rippledown.kb.resolveKbName
import io.rippledown.log.lazyLogger
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.UserId
import io.rippledown.model.external.ExternalCase
import io.rippledown.sample.SampleKB
import io.rippledown.server.websocket.WebSocketManager
import kotlinx.serialization.json.Json

private const val DEMO_CASE_RESOURCE = "/demo/Einstein.json"

private val jsonAllowSMK = Json {
    allowStructuredMapKeys = true
}

/**
 * The knowledge base operations as seen by one user: their open KB is the one
 * their chat is about, and the pushes that change what their window shows go
 * to them alone.
 */
class ApplicationKbService(
    private val application: ServerApplication,
    private val webSocketManager: WebSocketManager,
    private val userId: UserId,
    private val openKnowledgeBases: OpenKnowledgeBases,
    private val clock: () -> Long = System::currentTimeMillis
) : KnowledgeBaseService {
    private val logger = lazyLogger

    private fun openEndpoint() = openKnowledgeBases.openEndpointFor(userId)

    override fun knowledgeBases(): List<KBInfo> = application.kbList()

    override fun demonstrations(): List<SampleKB> = SampleKB.demonstrations()

    override fun isDemonstrationTitle(name: String): Boolean = isDemonstrationTitle(name, demonstrations())

    override fun openKnowledgeBase(): KBInfo? = openEndpoint()?.kbInfo()

    override fun resolve(name: String): KbResolution = resolveKbName(name, knowledgeBases(), demonstrations())

    override fun nearDuplicateOf(newName: String): KBInfo? = nearDuplicateOf(newName, knowledgeBases())

    override suspend fun open(kbInfo: KBInfo) {
        application.selectKB(kbInfo.id)
        webSocketManager.sendKbInfo(userId, kbInfo)
    }

    override suspend fun create(name: String): KBInfo {
        val created = application.createKB(name, force = false)
        webSocketManager.sendKbInfo(userId, created)
        return created
    }

    override suspend fun createFromSample(name: String, sample: SampleKB): KBInfo {
        val created = application.createKBFromSample(name, sample)
        webSocketManager.sendKbInfo(userId, created)
        return created
    }

    override suspend fun close() {
        logger.info("User '$userId' closing KB '${openKnowledgeBase()?.name}'.")
        openEndpoint()?.session?.release(userId)
        openKnowledgeBases.knowledgeBaseClosed(userId)
        webSocketManager.sendKbClosed(userId)
    }

    override suspend fun delete(kbInfo: KBInfo) {
        application.deleteKB(kbInfo.id, userId)
        val affected = openKnowledgeBases.knowledgeBaseDeleted(kbInfo.id)
        if (affected.isNotEmpty()) {
            logger.info("KB '${kbInfo.name}' deleted; closing it for users $affected.")
        }
        affected.forEach { webSocketManager.sendKbClosed(it) }
    }

    override suspend fun addDemonstrationCase(): RDRCase {
        val endpoint = checkNotNull(openEndpoint()) { "No knowledge base is open." }
        val case = endpoint.processCase(demonstrationCase())
        webSocketManager.sendCasesInfo(endpoint.waitingCasesInfo())
        return case
    }

    override suspend fun rename(newName: String): KBInfo {
        val endpoint = checkNotNull(openEndpoint()) { "No knowledge base is open." }
        val renamed = application.renameKB(endpoint.kbInfo().id, newName, userId)
        webSocketManager.sendKbInfo(userId, renamed)
        return renamed
    }

    override fun description(kbInfo: KBInfo): String = application.kbFor(kbInfo).description()

    override fun setDescription(kbInfo: KBInfo, text: String) {
        val endpoint = application.kbFor(kbInfo)
        endpoint.session.hold(userId)
        endpoint.setDescription(text)
    }

    override fun isRuleSessionActive() =
        openEndpoint()?.session?.ruleSessionManagerFor(userId)?.isRuleSessionActive() == true

    private fun demonstrationCase(): ExternalCase {
        val stream = checkNotNull(ApplicationKbService::class.java.getResourceAsStream(DEMO_CASE_RESOURCE)) {
            "Demonstration case resource $DEMO_CASE_RESOURCE is missing."
        }
        val text = stream.bufferedReader().use { it.readText() }
        return jsonAllowSMK.decodeFromString(ExternalCase.serializer(), text)
    }
}
