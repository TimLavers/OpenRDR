package steps

import io.cucumber.java.en.And
import io.cucumber.java.en.Then
import io.rippledown.constants.chat.ADD_A_COMMENT
import io.rippledown.constants.chat.PLEASE_CONFIRM
import io.rippledown.constants.chat.REPLACE
import io.rippledown.constants.chat.WOULD_YOU_LIKE
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds


class ChatDefs {
    @Then("I enter the following text into the chat panel:")
    fun enterChatTextAndSend(text: String) {
        with(chatPO()) {
            enterChatText(text)
            clickSend()
        }
    }

    @Then("I confirm")
    fun confirm() {
        with(chatPO()) {
            enterChatText("yes")
            clickSend()
        }
    }

    @Then("the chatbot has asked for confirmation")
    fun requireBotRequestForConfirmation() {
        await().atMost(ofSeconds(10)).until {
            chatPO().botRowContainsText(PLEASE_CONFIRM)
        }
    }

    @Then("the chatbot has asked if I want to add a comment")
    fun requireBotQuestionToAddAComment() {
        await().atMost(ofSeconds(10)).until {
            chatPO().botRowContainsText(WOULD_YOU_LIKE) &&
                    chatPO().botRowContainsText(ADD_A_COMMENT)
        }
    }

    @Then("the chatbot has asked if I want to change the report")
    fun requireBotQuestionToChangeTheReport() {
        await().atMost(ofSeconds(10)).until {
            chatPO().botRowContainsText(REPLACE)
        }
    }


    @And("I have added a comment {string} using the chat")
    fun addCommentUsingChat(comment: String) {
        requireBotQuestionToAddAComment()
        enterChatTextAndSend("Add the comment \"$comment\"")
        requireBotRequestForConfirmation()
        confirm()
    }


}