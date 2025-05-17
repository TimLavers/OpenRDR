package steps

import io.cucumber.java.en.Then
import io.rippledown.constants.chat.CONFIRMATION_START
import io.rippledown.constants.chat.QUESTION_IF_THERE_ARE_NO_EXISTING_COMMENTS
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds


class ChatDefs {
    @Then("I enter the into the chat panel the text:")
    fun enterChatTextAndSend(text: String) {
        with(chatPO()) {
            enterChatText(text)
            clickSend()
        }
    }

    @Then("the chatbot has asked for confirmation")
    fun requireBotRequestForConfirmation() {
        await().atMost(ofSeconds(10)).until {
            chatPO().botRowContainsText(CONFIRMATION_START)
        }
    }

    @Then("the chatbot has asked if I want to add a comment")
    fun requireBotQuestionToAddAComment() {
        await().atMost(ofSeconds(10)).until {
            chatPO().botRowContainsText(QUESTION_IF_THERE_ARE_NO_EXISTING_COMMENTS)
        }
    }

    @Then("I have confirmed that I want to add a comment to the report")
    fun confirmAddComment() {
        requireBotQuestionToAddAComment()
        with(chatPO()) {
            enterChatText("yes")
            clickSend()
        }
    }


}