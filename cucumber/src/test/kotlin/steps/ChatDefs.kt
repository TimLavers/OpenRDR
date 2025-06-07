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

    @Then("I decline")
    fun decline() {
        with(chatPO()) {
            enterChatText("no")
            clickSend()
        }
    }

    @Then("the chatbot has asked for confirmation")
    fun waitForBotRequestForConfirmation() {
        await().atMost(ofSeconds(10)).until {
            chatPO().mostRecentBotRowContainsTerms(listOf(PLEASE_CONFIRM))
        }
    }

    @Then("the chatbot has asked for confirmation of the comment:")
    fun waitForBotRequestForConfirmation(comment: String) {
        await().atMost(ofSeconds(10)).until {
            chatPO().mostRecentBotRowContainsTerms(listOf(PLEASE_CONFIRM, comment))
        }
    }

    @Then("the chatbot has completed the action")
    fun waitForBotToSayDone() {
        await().atMost(ofSeconds(10)).until {
            chatPO().mostRecentBotRowContainsTerms(listOf(CHAT_BOT_DONE_MESSAGE))
        }
    }

    @Then("the chatbot has asked for confirmation of the comment and condition")
    fun waitForBotRequestForConfirmationOfCommentAndCondition() {
        await().atMost(ofSeconds(10)).until {
            chatPO().mostRecentBotRowContainsTerms(
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
    fun waitForBotQuestionToProvideACondition() {
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
        mostRecentBotRowContainsTerms(listOf(WOULD_YOU_LIKE))
    }

    private fun botQuestionToAddAComment() = with(chatPO()) {
        mostRecentBotRowContainsTerms(listOf(WOULD_YOU_LIKE, ADD_A_COMMENT))
    }

    private fun botQuestionToProvideACondition() = with(chatPO()) {
        mostRecentBotRowContainsTerms(listOf(ANY_CONDITIONS))
    }

    @And("the chatbot has asked for what comment I want to add")
    fun waitForBotQuestionToSpecifyAComment() {
        await().atMost(ofSeconds(10)).until {
            botQuestionForWhatComment()
        }
    }

    private fun botQuestionForWhatComment() = with(chatPO()) {
        mostRecentBotRowContainsTerms(listOf(WHAT_COMMENT))
    }

    private fun botQuestionToAddRemoveOrReplaceAComment() = with(chatPO()) {
        mostRecentBotRowContainsTerms(listOf(WOULD_YOU_LIKE, ADD, REMOVE, REPLACE))
    }

    @And("I build a rule to add an initial comment {string} using the chat with no condition")
    fun addCommentUsingChat(comment: String) {
        waitForBotInitialPrompt()
        confirm()
        waitForBotQuestionToSpecifyAComment()
        enterChatTextAndSend("Add the comment: \"$comment\"")
        waitForBotRequestForConfirmation()
        confirm()
        waitForBotQuestionToProvideACondition()
        decline()
        waitForBotToSayDone()
    }

    @And("I build a rule to add another comment {string} using the chat")
    fun addAnotherCommentUsingChat(comment: String) {
        waitForBotQuestionToAddRemoveOrReplaceAComment()
        enterChatTextAndSend("Add the comment: \"$comment\"")
        waitForBotRequestForConfirmation()
        confirm()
        waitForBotQuestionToProvideACondition()
        decline()
        waitForBotToSayDone()
    }


}