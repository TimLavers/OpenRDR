package steps

import io.cucumber.datatable.DataTable
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
        waitForBotText(PLEASE_CONFIRM)
    }

    @Then("the chatbot has asked for confirmation of the comment:")
    fun waitForBotRequestForConfirmation(comment: String) {
        waitForBotText(PLEASE_CONFIRM, comment)
    }

    @Then("the chatbot has completed the action")
    fun waitForBotToSayDone() {
        waitForBotText(CHAT_BOT_DONE_MESSAGE)
    }

    fun waitForBotInitialPrompt() {
        waitForBotText(WOULD_YOU_LIKE)
    }

    @Then("the chatbot has asked if I want to add a comment")
    fun requireBotQuestionToAddAComment() {
        waitForBotText(WOULD_YOU_LIKE, ADD_A_COMMENT)
    }

    @And("the chatbot has asked if I want to provide any conditions")
    fun waitForBotQuestionToProvideConditions() {
        waitForBotText(ANY_CONDITIONS)
    }

    @And("the chatbot has asked if I want to provide any more conditions")
    fun waitForBotQuestionToProvideMoreConditions() {
        waitForBotText(ANY_MORE_CONDITIONS)
    }

    @And("the chatbot has asked for the first condition")
    fun waitForBotRequestForACondition() {
        waitForBotText(FIRST_CONDITION)
    }

    @And("the chatbot indicates that this condition is not true for the current case")
    fun waitForBotToIndicateThatConditionIsNotTrue() {
        waitForBotText(IS_NOT_TRUE)
    }

    @Then("the chatbot has asked if I want to add, remove or replace a comment")
    fun waitForBotQuestionToAddRemoveOrReplaceAComment() {
        waitForBotText(WOULD_YOU_LIKE, ADD, REMOVE, REPLACE)
    }

    @Then("the chatbot response contains the following phrases:")
    fun checkBotResponseContainsPhrases(phrases: DataTable) {
        waitForBotText(*phrases.asList().toTypedArray())
    }

    fun waitForBotText(vararg terms: String) {
        await().atMost(ofSeconds(10)).until {
            chatPO().mostRecentBotRowContainsTerms(terms.toList())
        }
    }

    @And("the chatbot has asked for what comment I want to add")
    fun waitForBotQuestionToSpecifyAComment() {
        waitForBotText(WHAT_COMMENT)
    }


    @And("I build a rule to add an initial comment {string} using the chat with no condition")
    fun addCommentUsingChat(comment: String) {
        waitForBotInitialPrompt()
        confirm()
        waitForBotQuestionToSpecifyAComment()
        enterChatTextAndSend("Add the comment: \"$comment\"")
        waitForBotRequestForConfirmation()
        confirm()
        waitForBotQuestionToProvideConditions()
        decline()
        waitForBotToSayDone()
    }

    @And("I build a rule to add another comment {string} using the chat")
    fun addAnotherCommentUsingChat(comment: String) {
        waitForBotQuestionToAddRemoveOrReplaceAComment()
        enterChatTextAndSend("Add the comment: \"$comment\"")
        waitForBotRequestForConfirmation()
        confirm()
        waitForBotQuestionToProvideConditions()
        decline()
        waitForBotToSayDone()
    }
}