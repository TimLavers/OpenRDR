package io.rippledown.kb.search

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.rippledown.kb.KBSession
import io.rippledown.kb.chat.OpenKnowledgeBases
import io.rippledown.model.CaseListType
import io.rippledown.model.KBInfo
import io.rippledown.model.UserId
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.GreaterThanButWithDelay
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.server.KBEndpoint
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.Test

class SearchCoordinatorTest {
    private val kbId = "coord"
    private val kb = io.rippledown.kb.KB(InMemoryKB(KBInfo(kbId, "Coordinator")))
    private val session = KBSession(kb)
    private val endpoint = KBEndpoint(session)
    private val tsh = kb.attributeManager.getOrCreate("TSH")
    private val tim = UserId("tim")

    // The users on the KB when a search completes, and the reports they received.
    private var usersOnKb = setOf(tim)
    private val reported = mutableListOf<Pair<Set<UserId>, String>>()

    private val openKnowledgeBases = object : OpenKnowledgeBases {
        override fun openEndpointFor(userId: UserId): KBEndpoint? = null
        override fun knowledgeBaseClosed(userId: UserId) {}
        override fun knowledgeBaseDeleted(kbId: String): Set<UserId> = emptySet()
        override fun usersOn(kbId: String): Set<UserId> = usersOnKb
    }

    @Test
    fun `runs a search in the background and reports its completion to the users on the KB`() {
        // Given two processed cases, one of which matches, and a coordinator that runs searches inline
        addCase("Case1", "2.1")
        addCase("Case2", "6.7")
        val coordinator = coordinator()

        // When a search is started
        coordinator.start(kbId, spec())

        // Then the search has run, and the users on the KB receive the report
        kb.userDefinedCaseLists().single().caseIds.map { it.name } shouldBe listOf("Case2")
        coordinator.isRunning(kbId) shouldBe false
        val (users, report) = reported.single()
        users shouldBe setOf(tim)
        report shouldContain "1 case found"
        report shouldContain "Processed"
        report shouldContain "\"TSH is greater than 4\""
        report shouldContain condition().asText()
        report shouldContain "found"
    }

    @Test
    fun `another search can be started once the previous one has completed`() {
        // Given one matching case and a coordinator that runs searches inline
        addCase("Case1", "6.7")
        val coordinator = coordinator()

        // When one search completes and another is started
        coordinator.start(kbId, spec())
        coordinator.start(kbId, spec(destination = "again"))

        // Then both ran, and the second could begin
        reported.size shouldBe 2
        kb.userDefinedCaseLists().map { it.name } shouldBe listOf("found", "again")
    }

    @Test
    fun `refuses a second search while one is in progress`() {
        // Given three slow cases and a coordinator that runs searches on their own threads
        addCase("Case1", "6.7")
        addCase("Case2", "8.9")
        addCase("Case3", "9.9")
        val coordinator = coordinator(background = true)

        // When a search is started and a second is requested before it finishes
        coordinator.start(kbId, spec(delayMillis = 150))

        // Then the second is refused
        shouldThrow<IllegalStateException> {
            coordinator.start(kbId, spec(destination = "second"))
        }.message shouldBe "A search is already in progress for this knowledge base."
        coordinator.isRunning(kbId) shouldBe true

        // And the first can be cancelled, allowing another to start
        coordinator.cancel(kbId) shouldBe true
        coordinator.awaitCompletion(kbId) shouldBe true
        coordinator.isRunning(kbId) shouldBe false
        coordinator.start(kbId, spec(destination = "second"))
    }

    @Test
    fun `refuses to start a search while a rule session is in progress`() {
        // Given a rule session in progress on the KB
        addCase("Case1", "6.7")
        val manager = session.ruleSessionManagerFor(tim)
        manager.startRuleSessionToAddComment(kb.getProcessedCaseByName("Case1"), "Whatever.")
        val coordinator = coordinator()

        // When a search is started
        // Then it is refused
        shouldThrow<IllegalStateException> {
            coordinator.start(kbId, spec())
        }.message shouldBe "Please finish or cancel the current rule before searching for cases."

        // And no search is running
        coordinator.isRunning(kbId) shouldBe false
        manager.cancelRuleSession()
    }

    @Test
    fun `cancelling retains the cases found so far and reports nothing`() {
        // Given three slow cases and a coordinator that runs searches on their own threads
        addCase("Case1", "6.7")
        addCase("Case2", "8.9")
        addCase("Case3", "9.9")
        val coordinator = coordinator(background = true)

        // When the search is cancelled after the first case has been found
        coordinator.start(kbId, spec(delayMillis = 100))
        waitFor(5_000) { kb.userDefinedCaseLists().isNotEmpty() }
        coordinator.cancel(kbId) shouldBe true
        coordinator.awaitCompletion(kbId) shouldBe true

        // Then the search is no longer running, the case found first is retained, and nothing is reported
        coordinator.isRunning(kbId) shouldBe false
        val retained = kb.userDefinedCaseLists().single().caseIds.size
        (retained in 1..2) shouldBe true
        reported shouldBe emptyList()
    }

    @Test
    fun `a chat turn can take the KB lock while a search is running`() {
        // Given a slow search in progress, which holds the KB lock per case
        addCase("Case1", "6.7")
        addCase("Case2", "8.9")
        addCase("Case3", "9.9")
        val coordinator = coordinator(background = true)
        coordinator.start(kbId, spec(delayMillis = 150))

        // When another thread takes the KB lock, as a chat turn would
        val acquired = CountDownLatch(1)
        thread { session.locked { acquired.countDown() } }

        // Then it gets the lock while the search is running, between cases
        acquired.await(5, TimeUnit.SECONDS) shouldBe true

        coordinator.cancel(kbId)
        coordinator.awaitCompletion(kbId)
    }

    @Test
    fun `the report is dropped when nobody is on the KB when the search completes`() {
        // Given nobody on the KB
        usersOnKb = emptySet()
        addCase("Case1", "6.7")
        val coordinator = coordinator()

        // When a search completes
        coordinator.start(kbId, spec())

        // Then the search ran, but nothing was reported to anyone
        kb.userDefinedCaseLists().single().caseIds.size shouldBe 1
        reported shouldBe emptyList()
    }

    private fun coordinator(background: Boolean = false): SearchCoordinator {
        val executor: Executor = if (background) {
            Executor { command -> thread { command.run() } }
        } else {
            Executor { it.run() }
        }
        return SearchCoordinator({ endpoint }, openKnowledgeBases, { users, text -> reported.add(users to text) }, executor)
    }

    private fun condition(delayMillis: Long? = null) = EpisodicCondition(
        null,
        tsh,
        if (delayMillis == null) io.rippledown.model.condition.episodic.predicate.GreaterThan(4.0) else GreaterThanButWithDelay(4.0, delayMillis),
        Current
    )

    private fun spec(
        destination: String = "found",
        delayMillis: Long? = null
    ) = CaseSearchSpec(
        listOf(ParsedSearchCondition("TSH is greater than 4", condition(delayMillis))),
        SearchConnector.ALL,
        null,
        listOf(CaseListType.Processed),
        destination
    )

    private fun addCase(name: String, tshValue: String) {
        val builder = io.rippledown.model.RDRCaseBuilder()
        builder.addValue(tsh, io.rippledown.utils.defaultDate, tshValue)
        kb.addProcessedCase(builder.build(name))
    }

    private fun waitFor(timeoutMillis: Long, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(10)
        }
        return false
    }
}
