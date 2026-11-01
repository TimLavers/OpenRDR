package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.After
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import io.rippledown.constants.api.API_URL
import io.rippledown.constants.api.CHANGES
import io.rippledown.constants.server.KB_ID
import io.rippledown.constants.server.LIMIT
import io.rippledown.main.Api
import io.rippledown.model.PresentUser
import io.rippledown.model.StaleRuleSessionException
import io.rippledown.model.UserId
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.change.ChangeKind
import io.rippledown.model.change.ChangeRecord
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.RuleConditionList
import io.rippledown.model.condition.greaterThanOrEqualTo
import io.rippledown.model.condition.lessThanOrEqualTo
import io.rippledown.model.diff.Addition
import io.rippledown.model.interpretationChangedMessage
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
import io.rippledown.model.rule.UndoRuleDescription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.assertj.swing.edt.GuiActionRunner.execute
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Several users talking to one server over REST with their own identities.
 * Presence scenarios also hold WebSockets open; the window steps use the UI clients.
 */
class ConcurrentUsersDefs {

    private class ChatUser(name: String) : AutoCloseable {
        private val engine = CIO.create()
        private val api = Api(engine = engine, userId = UserId(name))
        private val connectionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val connected = AtomicBoolean()
        private val connectionCount = AtomicInteger()
        var lastResponse: String = ""
        var lastCaseRead: ViewableCase? = null
        var lastChanges: List<ChangeRecord>? = null
        var lastUndoPreview: UndoRuleDescription? = null
        var lastUndoRefusal: String? = null
        var lastUndoStatus: HttpStatusCode? = null

        // Null if the last commit went through, else the server's sentence refusing it as stale.
        var lastCommitRefusal: String? = null

        // The case this user's rule session is about; committing needs its id.
        private lateinit var ruleSessionKb: String
        private lateinit var ruleSessionCase: String

        fun startConversation(kbName: String?) = runBlocking {
            val kbId = kbName?.let { name -> api.kbList().first { it.name == name }.id }
            lastResponse = api.startConversation(kbId, null).text
        }

        fun say(message: String): String = runBlocking {
            lastResponse = api.sendUserMessage(message).text
            lastResponse
        }

        fun connect() {
            connectionScope.launch {
                api.startWebSocketSession(
                    updateCornerstoneStatus = {},
                    ruleSessionCompleted = {},
                    connected = { open ->
                        if (open) connectionCount.incrementAndGet()
                        connected.set(open)
                    }
                )
            }
            await().atMost(ofSeconds(10)).until { connected.get() }
        }

        fun checkOriginalConnection() {
            // Allow a server-initiated close to reach the callback before declaring the socket intact.
            await().during(ofSeconds(1)).atMost(ofSeconds(3)).untilAsserted {
                connected.get() shouldBe true
                connectionCount.get() shouldBe 1
            }
        }

        fun presentUsers(kbName: String): List<PresentUser> = runBlocking {
            selectKB(kbName)
            api.presence()
        }

        fun readChanges(kbName: String, limit: Int) = runBlocking {
            val kb = selectKB(kbName)
            // Api.changes fetches every record for reconnect recovery; exercise the bounded REST query here.
            lastChanges = api.client.get("$API_URL$CHANGES") {
                parameter(KB_ID, kb.id)
                parameter(LIMIT, limit)
            }.body()
        }

        fun previewUndo(kbName: String) = runBlocking {
            selectKB(kbName)
            lastUndoPreview = api.lastRuleDescription()
        }

        fun confirmUndo() = runBlocking {
            lastUndoRefusal = null
            lastUndoStatus = null
            try {
                lastUndoStatus = api.undoLastRule()
            } catch (stale: StaleRuleSessionException) {
                lastUndoRefusal = stale.message
            }
        }

        fun startRuleSession(kbName: String, caseName: String, comment: String) = runBlocking {
            ruleSessionKb = kbName
            ruleSessionCase = caseName
            api.startRuleSession(SessionStartRequest(caseIdIn(kbName, caseName), Addition(comment)))
        }

        fun readCase(kbName: String, caseName: String) = runBlocking {
            lastCaseRead = requireNotNull(api.getCase(caseIdIn(kbName, caseName))) { "$caseName was not returned." }
        }

        fun commitRuleSession(conditionExpression: String?) = runBlocking {
            val caseId = caseIdIn(ruleSessionKb, ruleSessionCase)
            val conditions = listOfNotNull(conditionExpression?.let { conditionFor(caseId, it) })
            lastCommitRefusal = try {
                api.commitSession(RuleRequest(caseId, RuleConditionList(conditions)))
                null
            } catch (stale: StaleRuleSessionException) {
                stale.message
            }
        }

        // "TSH ≤ 1.0" or "TSH ≥ 10.0", built here rather than translated, so no LLM is involved.
        private suspend fun conditionFor(caseId: Long, expression: String): Condition {
            val (attributeName, operator, value) = expression.split(" ")
            val case = requireNotNull(api.getCase(caseId)) { "Case $caseId was not returned." }
            val attribute = case.attributes().first { it.name == attributeName }
            return when (operator) {
                "≤" -> lessThanOrEqualTo(null, attribute, value.toDouble())
                "≥" -> greaterThanOrEqualTo(null, attribute, value.toDouble())
                else -> error("Unknown operator in '$expression'.")
            }
        }

        fun commentGivenTo(kbName: String, caseName: String): String = runBlocking {
            requireNotNull(api.getCase(caseIdIn(kbName, caseName))) { "$caseName was not returned." }.latestText()
        }

        private suspend fun caseIdIn(kbName: String, caseName: String): Long {
            selectKB(kbName)
            return requireNotNull(api.waitingCasesInfo().caseIds.first { it.name == caseName }.id)
        }

        private suspend fun selectKB(kbName: String) = api.selectKB(api.kbList().first { it.name == kbName }.id)

        override fun close() {
            try {
                runBlocking {
                    withTimeout(10_000) { connectionScope.coroutineContext.job.cancelAndJoin() }
                }
            } finally {
                api.client.close()
                engine.close()
            }
        }
    }

    private val users = mutableMapOf<String, ChatUser>()

    private fun user(name: String) = users.getOrPut(name) { ChatUser(name) }

    // After hooks run in descending order: release sockets before Defs tears down the shared server.
    @After(order = 10)
    fun closeRestClients() {
        val results = users.values.map { runCatching { it.close() } }
        users.clear()
        results.forEach { it.getOrThrow() }
    }

    @Given("{word} starts the client application")
    fun startsTheClientApplication(userName: String) {
        StepsInfrastructure.startClientFor(userName)
    }

    @When("I switch to {word}'s window")
    fun switchToWindowOf(userName: String) {
        StepsInfrastructure.switchToClientOf(userName)
    }

    @Then("{word}'s window should show that {word} is also here")
    fun windowShowsOtherUser(userName: String, otherUser: String) {
        StepsInfrastructure.switchToClientOf(userName)
        await().atMost(ofSeconds(10)).until {
            execute<Boolean> { applicationBarPO().showPresence() }
        }
        try {
            await().atMost(ofSeconds(10)).untilAsserted {
                execute<String?> { applicationBarPO().presence() }.orEmpty().lines() shouldContain "• $otherUser"
            }
        } finally {
            execute { applicationBarPO().hidePresence() }
        }
    }

    @Then("{word}'s window should show a notice containing {string}")
    fun windowShowsNotice(userName: String, text: String) {
        StepsInfrastructure.switchToClientOf(userName)
        await().atMost(ofSeconds(10)).untilAsserted {
            chatPO().hasNoticeContaining(text) shouldBe true
        }
    }

    @Given("{word} starts a conversation about the knowledge base {word}")
    fun startConversationAboutKb(userName: String, kbName: String) {
        user(userName).startConversation(kbName)
    }

    @Given("{word} starts a conversation with no knowledge base open")
    fun startConversationWithNoKb(userName: String) {
        user(userName).startConversation(null)
    }

    @Given("{word} keeps a live connection to the server")
    fun keepsConnectionOpen(userName: String) {
        user(userName).connect()
    }

    @Then("{word}'s original connection to the server is still open")
    fun connectionIsStillOpen(userName: String) {
        user(userName).checkOriginalConnection()
    }

    @Then("{word} sees exactly these users present in the knowledge base {word}:")
    fun seesPresentUsers(userName: String, kbName: String, expected: DataTable) {
        val presentUsers = expected.asMaps().map {
            PresentUser(it.getValue("user"), it.getValue("editing").toBooleanStrict())
        }
        await().atMost(ofSeconds(10)).untilAsserted {
            user(userName).presentUsers(kbName) shouldContainExactlyInAnyOrder presentUsers
        }
    }

    @When("{word} reads the last {int} changes in the knowledge base {word}")
    fun readsChanges(userName: String, limit: Int, kbName: String) {
        user(userName).readChanges(kbName, limit)
    }

    @Then("{word}'s change history contains exactly these entries in order:")
    fun seesChangeHistory(userName: String, expected: DataTable) {
        val changes = requireNotNull(user(userName).lastChanges) { "$userName has not read the change history." }
        val entries = expected.asMaps()
        changes shouldHaveSize entries.size
        changes.zip(entries).forEach { (change, entry) ->
            withClue("Change ${change.id} in $userName's history") {
                change.userId shouldBe entry.getValue("user")
                change.kind shouldBe ChangeKind.valueOf(entry.getValue("kind"))
                change.summary shouldContain entry.getValue("summary contains")
            }
        }
    }

    @When("{word} previews undo in the knowledge base {word}")
    fun previewsUndo(userName: String, kbName: String) {
        user(userName).previewUndo(kbName)
    }

    @Then("{word}'s undo preview contains {string}")
    fun undoPreviewContains(userName: String, text: String) {
        val preview = requireNotNull(user(userName).lastUndoPreview) { "$userName has not previewed undo." }
        preview.canRemove shouldBe true
        preview.description shouldContain text
    }

    @When("{word} confirms the undo")
    fun confirmsUndo(userName: String) {
        user(userName).confirmUndo()
    }

    @Then("{word}'s undo refusal contains {string}")
    fun undoRefusalContains(userName: String, text: String) {
        user(userName).lastUndoRefusal shouldContain text
    }

    @Then("{word}'s undo succeeds")
    fun undoSucceeds(userName: String) {
        user(userName).lastUndoRefusal shouldBe null
        user(userName).lastUndoStatus shouldBe HttpStatusCode.OK
    }

    @When("{word} says {string} in the chat")
    fun says(userName: String, message: String) {
        user(userName).say(message)
    }

    @When("{word} asks the chat to delete the knowledge base {word}")
    fun asksToDeleteKb(userName: String, kbName: String) {
        with(user(userName)) {
            say("Delete the knowledge base $kbName") shouldContain "cannot be undone"
            say("yes") shouldContain "Deleted"
        }
    }

    @When("{word} asks the chat to close the knowledge base")
    fun asksToCloseKb(userName: String) {
        user(userName).say("Close the knowledge base") shouldContain "Closed"
    }

    @When("{word} starts a rule session on case {word} in the knowledge base {word}")
    fun startsRuleSession(userName: String, caseName: String, kbName: String) {
        user(userName).startRuleSession(kbName, caseName, "Go to Bondi.")
    }

    @When("{word} starts a rule session on case {word} in the knowledge base {word} to add {string}")
    fun startsRuleSessionToAdd(userName: String, caseName: String, kbName: String, comment: String) {
        user(userName).startRuleSession(kbName, caseName, comment)
    }

    @When("{word} reads case {word} in the knowledge base {word}")
    fun readsCase(userName: String, caseName: String, kbName: String) {
        user(userName).readCase(kbName, caseName)
    }

    @When("{word} commits his/her rule session with the condition {string}")
    fun commitsRuleSessionWithCondition(userName: String, conditionExpression: String) {
        user(userName).commitRuleSession(conditionExpression)
    }

    @When("{word} commits his/her rule session with no conditions")
    fun commitsRuleSessionWithNoConditions(userName: String) {
        user(userName).commitRuleSession(null)
    }

    @Then("{word}'s commit is refused because the interpretation of {word} changed")
    fun commitRefusedAsStale(userName: String, caseName: String) {
        user(userName).lastCommitRefusal shouldStartWith interpretationChangedMessage(caseName)
    }

    @Then("{word}'s commit refusal contains {string}")
    fun commitRefusalContains(userName: String, text: String) {
        user(userName).lastCommitRefusal shouldContain text
    }

    @Then("the comment given to case {word} in the knowledge base {word} is {string}")
    fun commentGivenToCase(caseName: String, kbName: String, comment: String) {
        user("Reader").commentGivenTo(kbName, caseName) shouldBe comment
    }

    @Then("{word} sees the {word} value {word} for case {word}")
    fun seesValue(userName: String, attributeName: String, value: String, caseName: String) {
        val case = requireNotNull(user(userName).lastCaseRead) { "$userName has not read a case." }
        case.name shouldBe caseName
        val attribute = case.attributes().first { it.name == attributeName }
        case.case.getLatest(attribute)?.value?.text shouldBe value
    }

    @Then("the chat response to {word} contains the following terms:")
    fun responseContains(userName: String, terms: DataTable) {
        val response = user(userName).lastResponse
        terms.asLists().flatten().forEach { term ->
            withClue("Response to $userName: \"$response\"") { response shouldContain term }
        }
    }

    @Then("the chat response to {word} does not contain {string}")
    fun responseDoesNotContain(userName: String, term: String) {
        val response = user(userName).lastResponse
        withClue("Response to $userName: \"$response\"") { response shouldNotContain term }
    }
}
