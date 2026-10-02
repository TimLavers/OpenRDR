package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.kotest.assertions.withClue
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.rippledown.main.Api
import io.rippledown.model.UserId
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

        fun startConversation(kbName: String?) = runBlocking {
            val kbId = kbName?.let { name -> api.kbList().first { it.name == name }.id }
            lastResponse = api.startConversation(kbId, null).text
        }

        fun say(message: String): String = runBlocking {
            lastResponse = api.sendUserMessage(message).text
            lastResponse
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
