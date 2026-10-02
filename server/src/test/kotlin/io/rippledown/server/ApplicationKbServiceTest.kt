package io.rippledown.server

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.rippledown.constants.chat.DEMO_CASE_NAME
import io.rippledown.kb.KbResolution
import io.rippledown.kb.chat.OpenKnowledgeBases
import io.rippledown.model.CasesInfo
import io.rippledown.model.KBInfo
import io.rippledown.model.UserId
import io.rippledown.model.diff.Addition
import io.rippledown.model.rule.SessionStartRequest
import io.rippledown.persistence.inmemory.InMemoryPersistenceProvider
import io.rippledown.sample.SampleKB
import io.rippledown.sample.SampleKB.ZOO
import io.rippledown.server.websocket.WebSocketManager
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class ApplicationKbServiceTest {
    private lateinit var webSocketManager: WebSocketManager
    private lateinit var app: ServerApplication
    private lateinit var openKnowledgeBases: FakeOpenKnowledgeBases
    private lateinit var service: ApplicationKbService
    private val alice = UserId("alice")
    private val bob = UserId("bob")
    private val now = 1_700_000_000_000L

    // The user's open KB is their chat context; the fake stands in for the coordinator.
    private class FakeOpenKnowledgeBases : OpenKnowledgeBases {
        val open = mutableMapOf<UserId, KBEndpoint>()
        val closed = mutableListOf<UserId>()
        val deleted = mutableListOf<String>()

        override fun openEndpointFor(userId: UserId) = open[userId]

        override fun knowledgeBaseClosed(userId: UserId) {
            closed += userId
            open.remove(userId)
        }

        override fun knowledgeBaseDeleted(kbId: String): Set<UserId> {
            deleted += kbId
            val affected = open.filterValues { it.kbInfo().id == kbId }.keys.toSet()
            affected.forEach { open.remove(it) }
            return affected
        }
    }

    private var openEndpoint: KBEndpoint?
        get() = openKnowledgeBases.open[alice]
        set(value) {
            if (value == null) openKnowledgeBases.open.remove(alice) else openKnowledgeBases.open[alice] = value
        }

    private val closedCount get() = openKnowledgeBases.closed.count { it == alice }

    private fun serviceFor(userId: UserId) =
        ApplicationKbService(app, webSocketManager, userId, openKnowledgeBases) { now }

    @BeforeEach
    fun setup() {
        webSocketManager = mockk()
        app = ServerApplication(InMemoryPersistenceProvider(), webSocketManager)
        openKnowledgeBases = FakeOpenKnowledgeBases()
        service = serviceFor(alice)
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
        service.resolve("Lipids") shouldBe KbResolution.NotFound(
            "Lipids", listOf("Thyroids"), SampleKB.demonstrations().map { it.title() }.sorted()
        )
        service.nearDuplicateOf("Thyroid") shouldBe thyroids
        service.nearDuplicateOf("Lipids").shouldBeNull()
    }

    @Test
    fun `resolve finds a demonstration when no stored knowledge base has its name`() {
        // Given
        app.createKB("Thyroids", false)

        // When
        val resolution = service.resolve("Zoo Animals")

        // Then
        resolution shouldBe KbResolution.Demonstration(ZOO)
    }

    @Test
    fun `demonstrations returns the sample recipes without storing knowledge bases`() {
        // Given
        val expected = SampleKB.demonstrations()

        // When
        val demonstrations = service.demonstrations()

        // Then
        demonstrations shouldBe expected
        app.kbList() shouldBe emptyList()
    }

    @Test
    fun `demonstration title check distinguishes demonstration and stored names`() {
        // Given
        app.createKB("Thyroids", false)
        val names = listOf("pathology", "Thyroids", " Zoo Animals ", "Zoo", " ")

        // When
        val results = names.map { service.isDemonstrationTitle(it) }

        // Then
        results shouldBe listOf(true, false, true, false, false)
    }

    @Test
    fun `a legacy stored exact name still takes precedence over a demonstration`() {
        // Given
        val persistence = InMemoryPersistenceProvider()
        val zoo = KBInfo("legacy_zoo", "Zoo Animals")
        persistence.createKBPersistence(zoo)
        app = ServerApplication(persistence, webSocketManager)
        service = serviceFor(alice)

        // When
        val resolution = service.resolve("Zoo Animals")

        // Then
        resolution shouldBe KbResolution.Exact(zoo)
    }

    @Test
    fun `create from sample builds the KB before pushing its KBInfo to the client`() = runBlocking<Unit> {
        // Given
        var processedCountWhenPushed: Int? = null
        coEvery { webSocketManager.sendKbInfo(alice, any()) } answers {
            processedCountWhenPushed = app.kbForId(secondArg<KBInfo>().id).kb.processedCaseIds().size
        }

        // When
        val created = service.createFromSample("Zoo2", ZOO)

        // Then
        created.name shouldBe "Zoo2"
        app.kbList() shouldBe listOf(created)
        app.kbForId(created.id).kb.processedCaseIds() shouldHaveSize 101
        app.kbForId(created.id).kb.ruleTree.size() shouldBe 18L
        processedCountWhenPushed shouldBe 101
        coVerify(exactly = 1) { webSocketManager.sendKbInfo(alice, created) }
    }

    @Test
    fun `create from sample refuses a name clash and pushes nothing`() = runBlocking<Unit> {
        // Given
        val existing = app.createKB("Zoo2", false)

        // When
        shouldThrow<IllegalArgumentException> {
            service.createFromSample("zoo2", ZOO)
        }

        // Then
        app.kbList() shouldBe listOf(existing)
        app.kbForId(existing.id).kb.processedCaseIds() shouldBe emptyList()
        coVerify(exactly = 0) { webSocketManager.sendKbInfo(any(), any()) }
    }

    @Test
    fun `open pushes the KBInfo to the opening user only`() = runBlocking<Unit> {
        // Given
        val thyroids = app.createKB("Thyroids", false)
        coEvery { webSocketManager.sendKbInfo(alice, thyroids) } just Runs

        // When
        service.open(thyroids)

        // Then
        coVerify(exactly = 1) { webSocketManager.sendKbInfo(alice, thyroids) }
        coVerify(exactly = 0) { webSocketManager.sendKbInfo(bob, any()) }
    }

    @Test
    fun `create makes the KB and pushes its KBInfo to the creating user only`() = runBlocking<Unit> {
        // Given
        val pushed = slot<KBInfo>()
        coEvery { webSocketManager.sendKbInfo(alice, capture(pushed)) } just Runs

        // When
        val created = service.create("Glucose")

        // Then
        created.name shouldBe "Glucose"
        app.kbList() shouldBe listOf(created)
        pushed.captured shouldBe created
        coVerify(exactly = 0) { webSocketManager.sendKbInfo(bob, any()) }
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
        coVerify(exactly = 0) { webSocketManager.sendKbInfo(any(), any()) }
    }

    @Test
    fun `close tells the closing user and changes nothing on the server`() = runBlocking<Unit> {
        // Given
        val thyroids = app.createKB("Thyroids", false)
        openEndpoint = app.kbForId(thyroids.id)
        coEvery { webSocketManager.sendKbClosed(alice) } just Runs

        // When
        service.close()

        // Then
        coVerify(exactly = 1) { webSocketManager.sendKbClosed(alice) }
        closedCount shouldBe 1
        openEndpoint.shouldBeNull()
        app.kbList() shouldBe listOf(thyroids)
    }

    @Test
    fun `one user closing does not disturb another user on the same KB`() = runBlocking<Unit> {
        // Given
        val thyroids = app.createKB("Thyroids", false)
        openKnowledgeBases.open[alice] = app.kbForId(thyroids.id)
        openKnowledgeBases.open[bob] = app.kbForId(thyroids.id)
        coEvery { webSocketManager.sendKbClosed(alice) } just Runs

        // When
        service.close()

        // Then
        serviceFor(bob).openKnowledgeBase() shouldBe thyroids
        openKnowledgeBases.closed shouldBe listOf(alice)
        coVerify(exactly = 0) { webSocketManager.sendKbClosed(bob) }
    }

    @Test
    fun `each user's open KB is their own`() {
        // Given
        val thyroids = app.createKB("Thyroids", false)
        val glucose = app.createKB("Glucose", false)
        openKnowledgeBases.open[alice] = app.kbForId(thyroids.id)
        openKnowledgeBases.open[bob] = app.kbForId(glucose.id)

        // When / Then
        service.openKnowledgeBase() shouldBe thyroids
        serviceFor(bob).openKnowledgeBase() shouldBe glucose
    }

    @Test
    fun `deleting a KB nobody has open closes nothing`() = runBlocking<Unit> {
        // Given
        val thyroids = app.createKB("Thyroids", false)
        val scratch = app.createKB("Scratch", false)
        openEndpoint = app.kbForId(thyroids.id)

        // When
        service.delete(scratch)

        // Then
        app.kbList() shouldBe listOf(thyroids)
        openKnowledgeBases.deleted shouldBe listOf(scratch.id)
        coVerify(exactly = 0) { webSocketManager.sendKbClosed(any()) }
        closedCount shouldBe 0
    }

    @Test
    fun `deleting the open KB also closes it`() = runBlocking<Unit> {
        // Given
        val thyroids = app.createKB("Thyroids", false)
        openEndpoint = app.kbForId(thyroids.id)
        coEvery { webSocketManager.sendKbClosed(alice) } just Runs

        // When
        service.delete(thyroids)

        // Then
        app.kbList() shouldBe emptyList()
        openEndpoint.shouldBeNull()
        coVerify(exactly = 1) { webSocketManager.sendKbClosed(alice) }
    }

    @Test
    fun `deleting a KB another user has open closes it for that user only`() = runBlocking<Unit> {
        // Given
        val thyroids = app.createKB("Thyroids", false)
        val glucose = app.createKB("Glucose", false)
        openKnowledgeBases.open[alice] = app.kbForId(thyroids.id)
        openKnowledgeBases.open[bob] = app.kbForId(glucose.id)
        coEvery { webSocketManager.sendKbClosed(bob) } just Runs

        // When
        service.delete(glucose)

        // Then
        app.kbList() shouldBe listOf(thyroids)
        service.openKnowledgeBase() shouldBe thyroids
        serviceFor(bob).openKnowledgeBase().shouldBeNull()
        coVerify(exactly = 1) { webSocketManager.sendKbClosed(bob) }
        coVerify(exactly = 0) { webSocketManager.sendKbClosed(alice) }
    }

    @Test
    fun `deleting the open KB removes it from the list before telling the client`() = runBlocking<Unit> {
        // Given
        val thyroids = app.createKB("Thyroids", false)
        openEndpoint = app.kbForId(thyroids.id)
        var listWhenClosed: List<KBInfo>? = null
        coEvery { webSocketManager.sendKbClosed(alice) } answers { listWhenClosed = app.kbList() }

        // When
        service.delete(thyroids)

        // Then
        listWhenClosed shouldBe emptyList()
    }

    @Test
    fun `rename updates the open KB and pushes the renamed KBInfo to the renaming user`() = runBlocking<Unit> {
        // given
        val thyroids = app.createKB("Thyroids", false)
        openEndpoint = app.kbForId(thyroids.id)
        val pushed = slot<KBInfo>()
        coEvery { webSocketManager.sendKbInfo(alice, capture(pushed)) } just Runs

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
    fun `description operations address the given KB, open or not`() {
        // given
        val thyroids = app.createKB("Thyroids", false)
        val glucose = app.createKB("Glucose", false)
        openEndpoint = app.kbForId(thyroids.id)

        // when
        service.setDescription(thyroids, "A thyroid knowledge base.")
        service.setDescription(glucose, "Glucose rules.")

        // then
        service.description(thyroids) shouldBe "A thyroid knowledge base."
        service.description(glucose) shouldBe "Glucose rules."
        app.kbForId(glucose.id).description() shouldBe "Glucose rules."
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
