package steps

import io.cucumber.java.en.And
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds

class ChatCaseDefs {
    @And("I copy the current case to the favourites case list")
    fun copyCaseToFavourites() {
        sendAndWaitForBotResponse("please copy the current case to favourites")
    }

    @And("I copy the current case to the favourites case list with name {string}")
    fun copyCaseToFavouritesWithName(newName: String) {
        sendAndWaitForBotResponse("please copy the current case to favourites with new name \"$newName\"")
    }

    @And("I delete the current case from the favourites case list")
    fun deleteCaseFromFavourites() {
        sendAndWaitForBotResponse("please delete the current case from favourites")
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
