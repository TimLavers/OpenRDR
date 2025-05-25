package steps

import io.cucumber.java.en.And
import io.cucumber.java.en.Then
import io.rippledown.constants.chat.*
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds


class ChatDefs {

    @Then("the chat is showing")
    fun showChat() {
        with(chatPO()) {
            clickChatIconToggle()
        }
    }

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
    fun waitForBotRequestForConfirmation() {
        await().atMost(ofSeconds(10)).until {
            chatPO().botRowContainsText(PLEASE_CONFIRM)
        }
    }

    @Then("the chatbot has asked for confirmation of the comment and condition")
    fun waitForBotRequestForConfirmationOfCommentAndCondition() {
        await().atMost(ofSeconds(10)).until {
            chatPO().botRowContainsTerms(
                listOf(
                    PLEASE_CONFIRM,
                    COMMENT,
                    ANY_CONDITIONS
                )
            )
        }
    }

    fun waitForBotInitialPrompt() {
        await().atMost(ofSeconds(10)).until {
            botInitialPrompt()
        }
    }

    @Then("the chatbot has asked if I want to add a comment")
    fun requireBotQuestionToAddAComment() {
        await().atMost(ofSeconds(10)).until {
            botQuestionToAddAComment()
        }
    }

    @Then("the chatbot has asked if I want to provide a condition")
    fun requireBotQuestionToProvideACondition() {
        await().atMost(ofSeconds(10)).until {
            botQuestionToProvideACondition()
        }
    }

    @Then("the chatbot has asked if I want to add, remove or replace a comment")
    fun waitForBotQuestionToAddRemoveOrReplaceAComment() {
        await().atMost(ofSeconds(10)).until {
            botQuestionToAddRemoveOrReplaceAComment()
        }
    }

    private fun botInitialPrompt() = with(chatPO()) {
        botRowContainsText(WOULD_YOU_LIKE)
    }

    private fun botQuestionToAddAComment() = with(chatPO()) {
        botRowContainsText(WOULD_YOU_LIKE) &&
                botRowContainsText(ADD_A_COMMENT)
    }

    private fun botQuestionToProvideACondition() = with(chatPO()) {
        botRowContainsText(WOULD_YOU_LIKE) &&
                botRowContainsText(ANY_CONDITIONS)
    }

    @And("the chatbot has asked for what comment I want to add")
    fun waitForBotQuestionToSpecifyAComment() {
        await().atMost(ofSeconds(10)).until {
            botQuestionFoWhatComment()
        }
    }

    private fun botQuestionFoWhatComment() = with(chatPO()) {
        botRowContainsText(WHAT_COMMENT)
    }

    private fun botQuestionToAddRemoveOrReplaceAComment() = with(chatPO()) {
        botRowContainsText(WOULD_YOU_LIKE) &&
                botRowContainsText(ADD) &&
                botRowContainsText(REMOVE) &&
                botRowContainsText(REPLACE)
    }

    @And("I build a rule to add an initial comment {string} using the chat")
    fun addCommentUsingChat(comment: String) {
        waitForBotInitialPrompt()
        confirm()
        enterChatTextAndSend("Add the comment: \"$comment\"")
        waitForBotRequestForConfirmation()
        confirm()
    }

    @And("I build a rule to add another comment {string} using the chat")
    fun addAnotherCommentUsingChat(comment: String) {
        waitForBotQuestionToAddRemoveOrReplaceAComment()
        confirm()
        enterChatTextAndSend("Add the comment: \"$comment\"")
        waitForBotRequestForConfirmation()
        confirm()
    }


}