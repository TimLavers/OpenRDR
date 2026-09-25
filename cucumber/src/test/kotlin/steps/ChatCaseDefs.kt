package steps

import io.cucumber.java.en.And
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds

class ChatCaseDefs {
    @And("I copy the current case to the {word} case list")
    fun copyCaseToUserDefinedCaseList(name: String) {
        sendAndWaitForBotResponse("please copy the current case to $name")
    }

    @And("I copy the current case to the {word} case list with name {string}")
    fun copyCaseToFavouritesWithName(listName: String, newName: String) {
        sendAndWaitForBotResponse("please copy the current case to $listName with new name \"$newName\"")
    }

    @And("I delete the current case from the {word} case list")
    fun deleteCaseFromUserDefinedCaseList(name: String) {
        sendAndWaitForBotResponse("please delete the current case from $name")
    }

    // The chat request is handled by a real LLM call, which completes
    // asynchronously. enterChatTextAndSend only submits the message, so we
    // must wait for the bot's reply (and hence for the server-side action -
    // e.g. copying the case to favourites - to have actually run) before
    // letting the scenario check the resulting state.
    private fun sendAndWaitForBotResponse(text: String) {
        val countBefore = chatPO().numberOfChatMessages()
        ChatDefs().enterChatTextAndSend(text)
        await().atMost(ofSeconds(90)).until {
            chatPO().numberOfChatMessages() > countBefore
        }
    }
}
