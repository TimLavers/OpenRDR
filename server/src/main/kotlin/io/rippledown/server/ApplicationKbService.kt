package io.rippledown.server

import io.rippledown.constants.chat.DEMO_CASE_NAME_MINIMAL
import io.rippledown.kb.KbResolution
import io.rippledown.kb.chat.DemonstrationCase
import io.rippledown.kb.chat.KnowledgeBaseService
import io.rippledown.kb.nearDuplicateOf
import io.rippledown.kb.resolveKbName
import io.rippledown.log.lazyLogger
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.Result
import io.rippledown.model.external.ExternalCase
import io.rippledown.model.external.MeasurementEvent
import io.rippledown.server.websocket.WebSocketManager
import kotlinx.serialization.json.Json

private const val PATHOLOGY_DEMO_CASE_RESOURCE = "/demo/Einstein.json"

private val jsonAllowSMK = Json {
    allowStructuredMapKeys = true
}

class ApplicationKbService(
    private val application: ServerApplication,
    private val webSocketManager: WebSocketManager,
    private val openEndpoint: () -> KBEndpoint?,
    private val clock: () -> Long = System::currentTimeMillis
) : KnowledgeBaseService {
    private val logger = lazyLogger

    override fun knowledgeBases(): List<KBInfo> = application.kbList()

    override fun openKnowledgeBase(): KBInfo? = openEndpoint()?.kbInfo()

    override fun resolve(name: String): KbResolution = resolveKbName(name, knowledgeBases())

    override fun nearDuplicateOf(newName: String): KBInfo? = nearDuplicateOf(newName, knowledgeBases())

    override suspend fun open(kbInfo: KBInfo) {
        application.selectKB(kbInfo.id)
        webSocketManager.sendKbInfo(kbInfo)
    }

    override suspend fun create(name: String): KBInfo {
        val created = application.createKB(name, force = false)
        webSocketManager.sendKbInfo(created)
        return created
    }

    override suspend fun close() {
        logger.info("Closing KB '${openKnowledgeBase()?.name}' on the client.")
        webSocketManager.sendKbClosed()
    }

    override suspend fun delete(kbInfo: KBInfo) {
        if (kbInfo == openKnowledgeBase()) close()
        application.deleteKB(kbInfo.id)
    }

    override suspend fun addDemonstrationCase(kind: DemonstrationCase): RDRCase {
        val endpoint = checkNotNull(openEndpoint()) { "No knowledge base is open." }
        val externalCase = when (kind) {
            DemonstrationCase.Pathology -> pathologyDemonstrationCase()
            DemonstrationCase.Minimal -> minimalDemonstrationCase()
        }
        val case = endpoint.processCase(externalCase)
        webSocketManager.sendCasesInfo(endpoint.waitingCasesInfo())
        return case
    }

    override fun isRuleSessionActive() = openEndpoint()?.session?.ruleSessionManager?.isRuleSessionActive() == true

    private fun pathologyDemonstrationCase(): ExternalCase {
        val stream = checkNotNull(ApplicationKbService::class.java.getResourceAsStream(PATHOLOGY_DEMO_CASE_RESOURCE)) {
            "Demonstration case resource $PATHOLOGY_DEMO_CASE_RESOURCE is missing."
        }
        val text = stream.bufferedReader().use { it.readText() }
        return jsonAllowSMK.decodeFromString(ExternalCase.serializer(), text)
    }

    private fun minimalDemonstrationCase() = ExternalCase(
        DEMO_CASE_NAME_MINIMAL,
        mapOf(MeasurementEvent("x", clock()) to Result("1"))
    )
}
