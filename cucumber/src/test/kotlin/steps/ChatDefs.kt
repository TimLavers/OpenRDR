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
        waitForBotText(CONFIRM)
    }

    @Then("the chatbot has asked for confirmation of the (comment|reason):")
    fun waitForBotRequestForConfirmation(textToConfirm: String) {
        waitForBotText(CONFIRM, textToConfirm)
    }

    @Then("the chatbot has completed the action")
    fun waitForBotToSayDone() {
        waitForBotText(CHAT_BOT_DONE_MESSAGE)
    }

    fun waitForBotInitialPrompt() {
        waitForBotText(WOULD_YOU_LIKE)
    }

    @Then("the chatbot has asked if I would like to add a comment")
    fun requireBotQuestionToAddAComment() {
        waitForBotText(WOULD_YOU_LIKE, ADD_A_COMMENT)
    }

    @And("the chatbot has asked if I want to provide any reasons")
    fun waitForBotQuestionToProvideReasons() {
        waitForBotText(REASON)
    }

    @And("the chatbot has asked if I want to provide any more reasons")
    fun waitForBotQuestionToProvideMoreReasons() {
        waitForBotText(MORE_REASONS)
    }

    @And("the chatbot has asked for the first reason")
    fun waitForBotRequestForFirstReason() {
        waitForBotText(FIRST_REASON)
    }

    @And("the chatbot indicates that this reason is not true for the current case")
    fun waitForBotToIndicateThatReasonIsNotTrue() {
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
        await().atMost(ofSeconds(30)).until {
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
        enterChatTextAndSend(comment)
        waitForBotRequestForConfirmation()
        confirm()
        waitForBotQuestionToProvideReasons()
        decline()
        waitForBotToSayDone()
    }

    @And("I start to build a rule using the chat to add the comment {string}")
    fun startToAddCommentUsingChat(comment: String) {
        waitForBotQuestionToAddRemoveOrReplaceAComment()
        enterChatTextAndSend("Add the comment: \"$comment\"")
        waitForBotRequestForConfirmation()
        confirm()
    }

    @And("I build a rule to add another comment {string} using the chat")
    fun addAnotherCommentUsingChat(comment: String) {
        waitForBotQuestionToAddRemoveOrReplaceAComment()
        enterChatTextAndSend("Add the comment: \"$comment\"")
        waitForBotRequestForConfirmation()
        confirm()
        waitForBotQuestionToProvideReasons()
        decline()
        waitForBotToSayDone()
    }

    @And("the chatbot has asked if I want to allow the report of the cornerstone case to change")
    fun waitForBotQuestionToAllowCornerstoneReportToChange() {
        waitForBotText("allow", "change", "cornerstone")
    }

}