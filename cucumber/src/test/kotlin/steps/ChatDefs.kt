package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.And
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.rippledown.constants.chat.*
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

    @Then("I allow")
    fun allow() {
        with(chatPO()) {
            enterChatText("allow")
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

    @And("the chatbot has provided some suggestions")
    fun waitForBotSuggestions() {
        waitForSuggestionText("1.")
    }

    @And("the chatbot has asked if I want to provide any (more )reasons and I decline")
    fun waitForBotQuestionToProvideReasonsThenDecline() {
        waitForBotSuggestions() //the suggestions always come after the model's question to provide a reason
        decline()
    }

    @And("the chatbot has asked if I want to provide any (more )reasons and I confirm")
    fun waitForBotQuestionToProvideReasonsThenConfirm() {
        waitForBotText(REASON)
        val countBefore = chatPO().numberOfChatMessages()
        confirm()
        await().atMost(ofSeconds(60)).until {
            chatPO().numberOfChatMessages() > countBefore + 1
        }
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
        await().atMost(ofSeconds(60)).until {
            chatPO().mostRecentBotRowContainsTerms(terms.toList())
        }
    }

    fun waitForSuggestionText(vararg terms: String) {
        await().atMost(ofSeconds(10)).until {
            chatPO().mostRecentSuggestionRowContainsTerms(terms.toList())
        }
    }

    @And("the chatbot has asked for what comment I want to add")
    fun waitForBotQuestionToSpecifyAComment() {
        waitForBotText(WHAT_COMMENT)
    }

    @And("I click the non-editable suggested condition {string}")
    fun clickTheSuggestedCondition(text: String) {
        waitForSuggestionText(text)
        chatPO().clickSuggestion(text)
    }

    @And("I enter the suggested condition index {int}")
    fun clickTheSuggestedConditionAtIndex(index: Int) {
        waitForSuggestionText("${index}. ")
        chatPO().enterChatText(index.toString())
        chatPO().clickSend()
    }

    @And("I click and add the non-editable suggested condition {string}")
    fun clickAndAddTheSuggestedCondition(text: String) {
        clickTheSuggestedCondition(text)
        chatPO().clickSend()
    }

    @And("The user text should be {string}")
    fun requireUserText(text: String) {
        await().atMost(ofSeconds(10)).until {
            chatPO().chatTextFieldContains(text)
        }
    }

    @And("I provide the following reason(s):")
    fun provideReasons(reasons: DataTable) {
        provideTheseReasons(reasons)
    }

    @And("I provide only the following reason(s):")
    fun provideReasonsThenDeclineToAddMore(reasons: DataTable) {
        provideTheseReasons(reasons)
        declineToAddMoreReasons()
        waitForBotToSayDone()
    }

    fun declineToAddMoreReasons() {
        waitForBotSuggestions()
        decline()
    }

    fun provideTheseReasons(reasons: DataTable) = provideTheseReasons(reasons.asList())

    fun provideTheseReasons(reasons: List<String>) {
        reasons.forEach { reason ->
            waitForBotSuggestions()
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


    @And("I add a comment {string}, allowing the report change to the cornerstone case")
    fun addCommentUsingChatAndAllowCornerstoneReportChange(comment: String) {
        requestCommentBeAdded(comment)
        waitForBotSuggestions()
        decline()
        waitForBotQuestionToAllowReportChangeToCornerstoneThenConfirm()
        waitForBotToSayDone()
    }

    @And("I request that the comment {string} be added")
    fun requestCommentBeAdded(comment: String) {
        waitForBotQuestion()
        addComment(comment)
    }

    fun addComment(comment: String) {
        enterChatTextAndSend("Add the comment: \"$comment\"")
        waitForBotRequestForConfirmationAndConfirm()
    }

    fun removeComment(comment: String) {
        enterChatTextAndSend("Remove the comment: \"$comment\"")
        waitForBotRequestForConfirmationAndConfirm()
    }

    @And("I request that the comment be removed")
    fun requestCommentBeRemoved() {
        waitForBotQuestion()
        enterChatTextAndSend("Remove the comment")
        waitForBotRequestForConfirmationAndConfirm()
    }

    @And("I request that the following comment be removed:")
    fun requestCommentBeRemoved(comment: String) {
        waitForBotQuestion()
        removeSpecificComment(comment)
    }

    fun removeSpecificComment(comment: String) {
        enterChatTextAndSend("Remove the comment: \"$comment\"")
        waitForBotRequestForConfirmationAndConfirm()
    }

    @And("I request that the comment be replaced by {string}")
    fun requestTheOnlyCommentBeReplacedBy(comment: String) {
        waitForBotQuestion()
        enterChatTextAndSend("Replace the comment by '$comment'")
        waitForBotRequestForConfirmationAndConfirm()
    }

    fun requestCommentBeReplacedBy(comment: String, replacement: String) {
        waitForBotQuestion()
        enterChatTextAndSend("Replace the '$comment' comment by '$replacement'")
        waitForBotRequestForConfirmationAndConfirm()
    }

    @And("the chatbot has asked if want to allow the report change to the cornerstone case and I confirm")
    fun waitForBotQuestionToAllowReportChangeToCornerstoneThenConfirm() {
        waitForBotQuestion()
        allow()
    }

    @And("the chatbot has asked if want to allow the report change to cornerstone case {string} and I confirm")
    fun waitForBotQuestionToAllowReportChangeToCornerstoneThenConfirm(name: String) {
        waitForBotText(name)
        allow()
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

    @When("I remove the condition {string}")
    fun removeTheCondition(text: String) {
        enterChatTextAndSend("Remove \"$text\"")
        waitForBotText("removed")
    }

    @And("I request that the {word} reason be removed")
    fun removeReason(index: String) {
        enterChatTextAndSend("Remove the $index reason")
    }

}