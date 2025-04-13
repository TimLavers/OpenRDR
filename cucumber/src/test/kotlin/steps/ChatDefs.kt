package steps

import io.cucumber.java.en.Then
import io.kotest.matchers.string.shouldContain

class ChatDefs {
    @Then("I enter the into the chat panel the text:")
    fun enterChatTextAndSend(text: String) {
        with(chatPO()) {
            enterChatText(text)
            clickSend()
        }
    }

    @Then("the chatbot responds with text containing the phrases:")
    fun requireChatConfirmation(expected: List<String>) {
        val chatText = chatPO().chatText()
        expected.forEach {
            chatText shouldContain it
        }
    }


}