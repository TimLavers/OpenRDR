package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.rippledown.main.Api
import io.rippledown.model.KnowledgeBaseHeldException
import io.rippledown.model.UserId
import io.rippledown.model.diff.Addition
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
import kotlinx.coroutines.runBlocking

/**
 * Several users talking to the one server, each over REST with their own
 * identity header. No GUI is involved: what a user sees is what the chat
 * answers them.
 */
class ConcurrentUsersDefs {

    private class ChatUser(name: String) {
        private val api = Api(userId = UserId(name))
        var lastResponse: String = ""

        // The outcome of the last REST request: null if it succeeded, else the server's refusal.
        var lastRefusal: String? = null

        fun startConversation(kbName: String?) = runBlocking {
            val kbId = kbName?.let { name -> api.kbList().first { it.name == name }.id }
            lastResponse = api.startConversation(kbId, null).text
        }

        fun say(message: String): String = runBlocking {
            lastResponse = api.sendUserMessage(message).text
            lastResponse
        }

        fun startRuleSession(kbName: String, caseName: String) = request {
            api.startRuleSession(SessionStartRequest(caseIdIn(kbName, caseName), Addition("Go to Bondi.")))
        }

        fun readCase(kbName: String, caseName: String) = request {
            requireNotNull(api.getCase(caseIdIn(kbName, caseName))) { "$caseName was not returned." }
        }

        fun cancelRuleSession() = request { api.cancelRuleSession() }

        fun commitRuleSession(kbName: String, caseName: String) = request {
            api.commitSession(RuleRequest(caseIdIn(kbName, caseName)))
        }

        fun commentGivenTo(kbName: String, caseName: String): String = runBlocking {
            requireNotNull(api.getCase(caseIdIn(kbName, caseName))) { "$caseName was not returned." }.latestText()
        }

        private suspend fun caseIdIn(kbName: String, caseName: String): Long {
            api.selectKB(api.kbList().first { it.name == kbName }.id)
            return requireNotNull(api.waitingCasesInfo().caseIds.first { it.name == caseName }.id)
        }

        private fun request(block: suspend () -> Unit) = runBlocking {
            lastRefusal = try {
                block()
                null
            } catch (held: KnowledgeBaseHeldException) {
                held.message
            }
        }
    }

    private val users = mutableMapOf<String, ChatUser>()

    // The case the most recently started rule session is about; committing needs its id.
    private lateinit var ruleSessionKb: String
    private lateinit var ruleSessionCase: String

    private fun user(name: String) = users.getOrPut(name) { ChatUser(name) }

    @Given("{word} starts a conversation about the knowledge base {word}")
    fun startConversationAboutKb(userName: String, kbName: String) {
        user(userName).startConversation(kbName)
    }

    @Given("{word} starts a conversation with no knowledge base open")
    fun startConversationWithNoKb(userName: String) {
        user(userName).startConversation(null)
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
        ruleSessionKb = kbName
        ruleSessionCase = caseName
        user(userName).startRuleSession(kbName, caseName)
    }

    @When("{word} reads case {word} in the knowledge base {word}")
    fun readsCase(userName: String, caseName: String, kbName: String) {
        user(userName).readCase(kbName, caseName)
    }

    @When("{word} cancels her rule session")
    fun cancelsRuleSession(userName: String) {
        user(userName).cancelRuleSession()
    }

    @When("{word} commits her rule session")
    fun commitsRuleSession(userName: String) {
        user(userName).commitRuleSession(ruleSessionKb, ruleSessionCase)
    }

    @Then("the comment given to case {word} in the knowledge base {word} is {string}")
    fun commentGivenToCase(caseName: String, kbName: String, comment: String) {
        user("Reader").commentGivenTo(kbName, caseName) shouldBe comment
    }

    @Then("{word}'s request succeeds")
    fun requestSucceeds(userName: String) {
        withClue("$userName's last request") { user(userName).lastRefusal.shouldBeNull() }
    }

    @Then("{word}'s request is refused with {string}")
    fun requestRefused(userName: String, refusal: String) {
        val actual = user(userName).lastRefusal
        withClue("$userName's last request") { actual.shouldNotBeNull() }
        actual shouldBe refusal
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
