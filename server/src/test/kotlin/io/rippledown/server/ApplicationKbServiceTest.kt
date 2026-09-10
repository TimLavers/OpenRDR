package io.rippledown.server

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.rippledown.constants.chat.DEMO_CASE_NAME
import io.rippledown.kb.KbResolution
import io.rippledown.model.CasesInfo
import io.rippledown.model.KBInfo
import io.rippledown.model.diff.Addition
import io.rippledown.model.rule.SessionStartRequest
import io.rippledown.persistence.inmemory.InMemoryPersistenceProvider
import io.rippledown.server.websocket.WebSocketManager
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class ApplicationKbServiceTest {
    private lateinit var webSocketManager: WebSocketManager
    private lateinit var app: ServerApplication
    private var openEndpoint: KBEndpoint? = null
    private var closedCount = 0
    private lateinit var service: ApplicationKbService
    private val now = 1_700_000_000_000L

    @BeforeEach
    fun setup() {
        webSocketManager = mockk()
        app = ServerApplication(InMemoryPersistenceProvider(), webSocketManager)
        openEndpoint = null
        closedCount = 0
        service = ApplicationKbService(app, webSocketManager, { openEndpoint }, { closedCount++ }, { now })
    }

    @Test
    fun `knowledge bases are listed sorted by name`() {
        // Given
        val thyroids = app.createKB("Thyroids", false)
        val glucose = app.createKB("Glucose", false)

        // When / Then
        service.knowledgeBases() shouldBe listOf(glucose, thyroids)
    }

    @Test
    fun `no open knowledge base`() {
        // Given
        app.createKB("Thyroids", false)

        // When / Then
        service.openKnowledgeBase().shouldBeNull()
    }

    @Test
    fun `the open knowledge base is that of the current endpoint`() {
        // Given
        val thyroids = app.createKB("Thyroids", false)
        openEndpoint = app.kbForId(thyroids.id)

        // When / Then
        service.openKnowledgeBase() shouldBe thyroids
    }

    @Test
    fun `resolve and nearDuplicateOf work against the current list`() {
        // Given
        val thyroids = app.createKB("Thyroids", false)

        // When / Then
        service.resolve("thyroids") shouldBe KbResolution.Exact(thyroids)
        service.resolve("thyroid") shouldBe KbResolution.Partial(thyroids)
        service.resolve("Lipids") shouldBe KbResolution.NotFound("Lipids", listOf("Thyroids"))
        service.nearDuplicateOf("Thyroid") shouldBe thyroids
        service.nearDuplicateOf("Lipids").shouldBeNull()
    }

    @Test
    fun `open pushes the KBInfo to the client`() = runBlocking<Unit> {
        // Given
        val thyroids = app.createKB("Thyroids", false)
        coEvery { webSocketManager.sendKbInfo(thyroids) } just Runs

        // When
        service.open(thyroids)

        // Then
        coVerify(exactly = 1) { webSocketManager.sendKbInfo(thyroids) }
    }

    @Test
    fun `create makes the KB and pushes its KBInfo to the client`() = runBlocking<Unit> {
        // Given
        val pushed = slot<KBInfo>()
        coEvery { webSocketManager.sendKbInfo(capture(pushed)) } just Runs

        // When
        val created = service.create("Glucose")

        // Then
        created.name shouldBe "Glucose"
        app.kbList() shouldBe listOf(created)
        pushed.captured shouldBe created
    }

    @Test
    fun `create refuses a name that clashes ignoring case and pushes nothing`() = runBlocking<Unit> {
        // Given
        app.createKB("Glucose", false)

        // When / Then
        shouldThrow<IllegalArgumentException> {
            service.create("glucose")
        }
        app.kbList().map { it.name } shouldBe listOf("Glucose")
        coVerify(exactly = 0) { webSocketManager.sendKbInfo(any()) }
    }

    @Test
    fun `close tells the client and changes nothing on the server`() = runBlocking<Unit> {
        // Given
        val thyroids = app.createKB("Thyroids", false)
        openEndpoint = app.kbForId(thyroids.id)
        coEvery { webSocketManager.sendKbClosed() } just Runs

        // When
        service.close()

        // Then
        coVerify(exactly = 1) { webSocketManager.sendKbClosed() }
        closedCount shouldBe 1
        app.kbList() shouldBe listOf(thyroids)
    }

    @Test
    fun `deleting a KB that is not open does not close anything`() = runBlocking<Unit> {
        // Given
        val thyroids = app.createKB("Thyroids", false)
        val scratch = app.createKB("Scratch", false)
        openEndpoint = app.kbForId(thyroids.id)

        // When
        service.delete(scratch)

        // Then
        app.kbList() shouldBe listOf(thyroids)
        coVerify(exactly = 0) { webSocketManager.sendKbClosed() }
        closedCount shouldBe 0
    }

    @Test
    fun `deleting the open KB also closes it`() = runBlocking<Unit> {
        // Given
        val thyroids = app.createKB("Thyroids", false)
        openEndpoint = app.kbForId(thyroids.id)
        coEvery { webSocketManager.sendKbClosed() } just Runs

        // When
        service.delete(thyroids)

        // Then
        app.kbList() shouldBe emptyList()
        coVerify(exactly = 1) { webSocketManager.sendKbClosed() }
        closedCount shouldBe 1
    }

    @Test
    fun `deleting the open KB removes it from the list before telling the client`() = runBlocking<Unit> {
        // Given
        val thyroids = app.createKB("Thyroids", false)
        openEndpoint = app.kbForId(thyroids.id)
        var listWhenClosed: List<KBInfo>? = null
        coEvery { webSocketManager.sendKbClosed() } answers { listWhenClosed = app.kbList() }

        // When
        service.delete(thyroids)

        // Then
        listWhenClosed shouldBe emptyList()
    }

    @Test
    fun `rename updates the open KB and pushes the renamed KBInfo`() = runBlocking<Unit> {
        // given
        val thyroids = app.createKB("Thyroids", false)
        openEndpoint = app.kbForId(thyroids.id)
        val pushed = slot<KBInfo>()
        coEvery { webSocketManager.sendKbInfo(capture(pushed)) } just Runs

        // when
        val renamed = service.rename("Thyroid Function")

        // then
        renamed shouldBe KBInfo(thyroids.id, "Thyroid Function")
        renamed.name shouldBe "Thyroid Function"
        openEndpoint?.kbInfo() shouldBe renamed
        app.kbList() shouldBe listOf(renamed)
        pushed.captured shouldBe renamed
        pushed.captured.name shouldBe "Thyroid Function"
    }

    @Test
    fun `description operations use the open KB`() {
        // given
        val thyroids = app.createKB("Thyroids", false)
        openEndpoint = app.kbForId(thyroids.id)

        // when
        service.setDescription("A thyroid knowledge base.")

        // then
        service.description() shouldBe "A thyroid knowledge base."
    }

    @Test
    fun `adding the demonstration case stores Einstein and pushes the cases info`() = runBlocking<Unit> {
        // Given
        val thyroids = app.createKB("Thyroids", false)
        openEndpoint = app.kbForId(thyroids.id)
        val pushed = slot<CasesInfo>()
        coEvery { webSocketManager.sendCasesInfo(capture(pushed)) } just Runs

        // When
        val case = service.addDemonstrationCase()

        // Then
        case.name shouldBe DEMO_CASE_NAME
        case.attributes shouldHaveSize 65
        case.attributes.map { it.name } shouldContainAll listOf("Patient Name", "HAEMOGLOBIN", "MCV", "TSH")
        case.dates shouldHaveSize 2
        app.kbForId(thyroids.id).kb.processedCaseIds().map { it.name } shouldBe listOf(DEMO_CASE_NAME)
        pushed.captured.caseIds.map { it.name } shouldBe listOf(DEMO_CASE_NAME)
        pushed.captured.kbName shouldBe "Thyroids"
    }

    @Test
    fun `adding a demonstration case with no open KB is an error`() = runBlocking<Unit> {
        // Given
        app.createKB("Thyroids", false)

        // When / Then
        shouldThrow<IllegalStateException> {
            service.addDemonstrationCase()
        }
    }

    @Test
    fun `no rule session without an open KB`() {
        // When / Then
        service.isRuleSessionActive() shouldBe false
    }

    @Test
    fun `rule session activity is that of the open KB`() = runBlocking<Unit> {
        // Given
        val thyroids = app.createKB("Thyroids", false)
        val endpoint = app.kbForId(thyroids.id)
        openEndpoint = endpoint
        coEvery { webSocketManager.sendCasesInfo(any()) } just Runs
        val case = service.addDemonstrationCase()
        service.isRuleSessionActive() shouldBe false

        // When
        endpoint.startRuleSession(SessionStartRequest(requireNotNull(case.caseId.id), Addition("Go to Bondi.")))

        // Then
        service.isRuleSessionActive() shouldBe true
    }
}
