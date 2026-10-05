package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.rippledown.main.Api
import io.rippledown.model.StaleRuleSessionException
import io.rippledown.model.UserId
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.RuleConditionList
import io.rippledown.model.condition.greaterThanOrEqualTo
import io.rippledown.model.condition.lessThanOrEqualTo
import io.rippledown.model.diff.Addition
import io.rippledown.model.interpretationChangedMessage
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
        var lastCaseRead: ViewableCase? = null

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
            api.selectKB(api.kbList().first { it.name == kbName }.id)
            return requireNotNull(api.waitingCasesInfo().caseIds.first { it.name == caseName }.id)
        }
    }

    private val users = mutableMapOf<String, ChatUser>()

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
        user(userName).lastCommitRefusal shouldBe interpretationChangedMessage(caseName)
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
