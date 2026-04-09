package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.And
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.rippledown.constants.chat.*
import io.rippledown.constants.rule.UNDERSTAND
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

    fun confirm() {
        with(chatPO()) {
            enterChatText("yes")
            clickSend()
        }
    }

    fun allow() {
        with(chatPO()) {
            enterChatText("allow")
            clickSend()
        }
    }

    fun decline() {
        with(chatPO()) {
            enterChatText("no")
            clickSend()
        }
    }

    fun waitForBotRequestForConfirmation() {
        waitForBotText(CONFIRM)
    }

    fun waitForBotRequestForConfirmationAndConfirm() {
        waitForBotText(CONFIRM)
        confirm()
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

    fun waitForBotSuggestions() {
        waitForSuggestionText("1.")
    }

    @And("the chatbot has asked if I want to provide any (more )reasons and I decline")
    fun waitForBotQuestionToProvideReasonsThenDecline() {
        //"1." is the start of a suggestion
        waitForBotQuestionToProvideAnotherReasonOrGiveSuggestions()
        decline()
    }

    fun waitForBotQuestionToProvideAnotherReasonOrGiveSuggestions() {
        waitForBotTextToContainAnyOf(REASON, SUGGESTION, "1.")
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

    @And("the chatbot indicates that this reason is not true for the current case")
    fun waitForBotToIndicateThatReasonIsNotTrue() {
        waitForBotText(IS_NOT_TRUE)
    }

    @Then("the chatbot has asked if I want to add, remove or replace a comment")
    fun waitForBotQuestionToAddRemoveOrReplaceAComment() {
        waitForBotText(WOULD_YOU_LIKE, ADD, REMOVE, REPLACE)
    }

    @Then("the chatbot response does not contain {string}")
    fun requireChatbotResponseToNotContain(text: String) {
        chatPO().mostRecentBotRowDoesNotContainTheTerm(text)
    }

    fun waitForBotText(vararg terms: String) {
        await().atMost(ofSeconds(60)).until {
            chatPO().mostRecentBotRowContainsTerms(terms.toList())
        }
    }
    fun waitForBotTextToContainAnyOf(vararg terms: String) {
        await().atMost(ofSeconds(60)).until {
            chatPO().mostRecentBotRowContainsAnyOfTheTerms(terms.toList())
        }
    }

    fun waitForSuggestionText(vararg terms: String) {
        await().atMost(ofSeconds(60)).until {
            chatPO().mostRecentSuggestionRowContainsTerms(terms.toList())
        }
    }

    fun waitForNewSuggestions(countBefore: Int) {
        await().atMost(ofSeconds(60)).until {
            chatPO().numberOfSuggestionRows() > countBefore
        }
    }
    fun waitForPromptToProvideAnotherReason(countBefore: Int) {
        await().atMost(ofSeconds(60)).until {
            chatPO().numberOfSuggestionRows() > countBefore
        }
    }

    fun waitForBotResponseIndicatingInvalidReason() {
        waitForBotTextToContainAnyOf(UNDERSTAND, "means")
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
        waitForBotQuestionToProvideReasonsThenDecline()
    }

    fun provideTheseReasons(reasons: DataTable) {
        provideTheseReasons(reasons.asLists().map { it[0].trim() })
    }

    fun provideTheseReasons(reasons: List<String>) {
        var previousSuggestionCount = 0
        var messageCountAfterSend = 0
        reasons.forEachIndexed { index, reason ->
            if (index == 0) {
                waitForBotSuggestions()
            } else {
                waitForBotResponseToReason(previousSuggestionCount, messageCountAfterSend)
            }
            previousSuggestionCount = chatPO().numberOfSuggestionRows()
            enterChatTextAndSend(reason)
            messageCountAfterSend = chatPO().numberOfChatMessages()
        }
    }

    private fun waitForBotResponseToReason(previousSuggestionCount: Int, messageCountAfterSend: Int) {
        await().atMost(ofSeconds(60)).until {
            chatPO().numberOfSuggestionRows() > previousSuggestionCount ||
                    chatPO().numberOfChatMessages() > messageCountAfterSend
        }
    }

    @And("I add a comment {string}, allowing the report change to the cornerstone case")
    fun addCommentUsingChatAndAllowCornerstoneReportChange(comment: String) {
        buildRuleToAddCommentAllowingCC(comment)
    }

    @And("I add another comment {string}, allowing the report change to the cornerstone case")
    fun addAnotherCommentUsingChatAndAllowCornerstoneReportChange(comment: String) {
        buildRuleToAddCommentAllowingCC(comment, false)
    }

    private fun buildRuleToAddCommentAllowingCC(comment: String, waitForBotQuestionFirst: Boolean = true) {
        if (waitForBotQuestionFirst) {
            waitForBotQuestion()
        }
        waitForBotQuestion()
        addCommentWithoutConfirmation(comment)
        waitForBotSuggestions()
        decline()
        waitForBotQuestionToAllowReportChangeToCornerstoneThenConfirm()
        waitForBotSuggestions()
        decline()
        waitForBotToSayDone()
    }

    @And("I request that the comment {string} be added")
    fun requestCommentBeAdded(comment: String) {
        waitForBotQuestion()
        addCommentWithoutConfirmation(comment)
    }

    fun addCommentWithoutConfirmation(comment: String) {
        enterChatTextAndSend("Add the comment: \"$comment\"")
    }

    fun removeCommentWithoutConfirmation(comment: String) {
        waitForBotQuestion()
        enterChatTextAndSend("Remove the comment: \"$comment\"")
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
        removeSpecificCommentWithoutConfirmation(comment)
    }

    fun removeSpecificCommentWithoutConfirmation(comment: String) {
        enterChatTextAndSend("Remove the comment: \"$comment\"")
    }

    @And("I request that the comment be replaced by {string}")
    fun requestTheOnlyCommentBeReplacedWithoutConfirmationBy(comment: String) {
        waitForBotQuestion()
        enterChatTextAndSend("Replace the comment by \"$comment\"")
    }

    fun requestCommentBeReplacedWithoutConfirmationBy(comment: String, replacement: String) {
        waitForBotQuestion()
        enterChatTextAndSend("Replace the comment \"$comment\" by \"$replacement\"")
    }

    @And("the chatbot has asked if want to allow the report change to the cornerstone case and I confirm")
    fun waitForBotQuestionToAllowReportChangeToCornerstoneThenConfirm() {
        waitForBotQuestion()
        allow()
    }

    @And("the chatbot has asked if want to allow the report change to the cornerstone case and I decline")
    fun waitForBotQuestionToAllowReportChangeToCornerstoneThenDecline() {
        waitForBotQuestion()
        decline()
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
        waitForBotQuestionToProvideAnotherReasonOrGiveSuggestions()
        enterChatTextAndSend("Remove \"$text\"")
        waitForBotTextToContainAnyOf("removed", SUGGESTION, "1.")
    }

    @And("I request that the {word} reason be removed")
    fun removeReason(index: String) {
        enterChatTextAndSend("Remove the $index reason")
    }

    @When("I ask the chatbot to show the next cornerstone case")
    fun askToShowNextCornerstoneCase() {
        waitForBotQuestion()
        enterChatTextAndSend("show me the next cornerstone case")
    }

    @When("I ask the chatbot to show the previous cornerstone case")
    fun askToShowPreviousCornerstoneCase() {
        waitForBotQuestion()
        enterChatTextAndSend("show me the previous cornerstone case")
    }

    @Then("the chatbot has mentioned the cornerstone case {string}")
    fun waitForBotToMentionCornerstoneCase(name: String) {
        waitForBotText(name)
    }

}