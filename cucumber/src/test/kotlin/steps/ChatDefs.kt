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

    @Then("the chatbot has asked for confirmation and I confirm")
    fun waitForBotRequestForConfirmationAndConfirm() {
        waitForBotText(CONFIRM)
        confirm()
    }

    @Then("the chatbot has asked for confirmation of the (comment|reason):")
    fun waitForBotRequestForConfirmation(textToConfirm: String) {
        waitForBotText(CONFIRM, textToConfirm)
    }

    @Then("the chatbot has completed the action")
    fun waitForBotToSayDone() {
        waitForBotText(CHAT_BOT_DONE_MESSAGE)
    }

    fun waitForBotQuestion() {
        waitForBotText("?")
    }

    @Then("the chatbot has asked if I would like to add a comment")
    fun requireBotQuestionToAddAComment() {
        waitForBotText(WOULD_YOU_LIKE, ADD_A_COMMENT)
    }

    @And("the chatbot has asked if I want to provide any (more )reasons")
    fun waitForBotQuestionToProvideReasons() {
        waitForBotText(REASON)
    }

    @And("the chatbot has asked if I want to provide any (more )reasons and I decline")
    fun waitForBotQuestionToProvideReasonsThenDecline() {
        waitForBotText(REASON)
        decline()
    }

    @And("the chatbot has asked if I want to provide any (more )reasons and I confirm")
    fun waitForBotQuestionToProvideReasonsThenConfirm() {
        waitForBotText(REASON)
        confirm()
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
        waitForBotQuestion()
        confirm()
        waitForBotQuestionToSpecifyAComment()
        enterChatTextAndSend(comment)
        waitForBotRequestForConfirmation()
        confirm()
        waitForBotQuestionToProvideReasons()
        decline()
        waitForBotToSayDone()
    }

    @And("I provide only the following reason(s):")
    fun provideTheseReasonsThenDeclineToAddMore(reasons: DataTable) {
        provideTheseReasons(reasons)
        declineToAddMoreReasons()
        waitForBotToSayDone()
    }

    fun declineToAddMoreReasons() {
        waitForBotQuestionToProvideReasons()
        decline()
    }

    @And("I provide the following reason(s):")
    fun provideTheseReasons(reasons: DataTable) {
        reasons.asList().forEach { reason ->
            waitForBotQuestionToProvideReasons()
            enterChatTextAndSend(reason)
        }
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
        requestCommentBeAdded(comment)
        waitForBotQuestionToProvideReasons()
        decline()
        waitForBotToSayDone()
    }

    @And("I add a comment {string}, allowing the report change to the cornerstone case")
    fun addCommentUsingChatAndAllowCornerstoneReportChange(comment: String) {
        requestCommentBeAdded(comment)
        waitForBotQuestionToProvideReasons()
        decline()
        waitForBotQuestionToAllowReportChangeToCornerstoneThenConfirm()
        waitForBotToSayDone()
    }

    @And("I request that the comment {string} be added")
    fun requestCommentBeAdded(comment: String) {
        waitForBotQuestion()
        enterChatTextAndSend("Add the comment: \"$comment\"")
        waitForBotRequestForConfirmationAndConfirm()
    }

    @And("I request that the comment be removed")
    fun requestCommentBeRemoved() {
        waitForBotQuestion()
        enterChatTextAndSend("Remove the comment")
        waitForBotRequestForConfirmationAndConfirm()
    }

    @And("I request that the comment be replaced by {string}")
    fun requestCommentBeReplacedBy(comment: String) {
        waitForBotQuestion()
        enterChatTextAndSend("Replace the comment by '$comment'")
        waitForBotRequestForConfirmationAndConfirm()
    }

    @And("the chatbot has asked if want to allow the report change to the cornerstone case and I confirm")
    fun waitForBotQuestionToAllowReportChangeToCornerstoneThenConfirm() {
        waitForBotQuestion()
        confirm()
    }

    @And("the chatbot has asked if want to allow the report change to cornerstone case {string} and I confirm")
    fun waitForBotQuestionToAllowReportChangeToCornerstoneThenConfirm(name: String) {
        waitForBotText(name)
        confirm()
    }

    @And("the chatbot lists the following reasons:")
    fun waitForReasonsToBeListed(dataTable: DataTable) {
        val reasons = dataTable.asList().toTypedArray()
        waitForBotText(*reasons)
    }

    @And("I ask to see the reasons")
    fun askToSeeReasons() {
        enterChatTextAndSend("What reasons are there?")
    }

    @And("I request that the {word} reason be removed")
    fun removeReason(index: String) {
        enterChatTextAndSend("Remove the $index reason")
    }

}