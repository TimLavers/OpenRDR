package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.And
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.kotest.matchers.shouldBe
import io.rippledown.constants.chat.*
import io.rippledown.constants.rule.UNDERSTAND
import org.awaitility.Awaitility.await
import org.awaitility.core.ConditionTimeoutException
import java.time.Duration.ofSeconds


class ChatDefs {

    @Then("I enter the following text into the chat panel:")
    fun enterChatTextAndSend(text: String) {
        with(chatPO()) {
            enterChatText(text)
            clickSend()
        }
    }

    @And("I confirm")
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

    /**
     * Wait until the bot is ready for a new request: either it has asked a
     * question (e.g. the opening greeting) or it has just completed the
     * previous action (its "done" message does not end with a question).
     */
    fun waitForBotQuestionOrCompletedAction() {
        waitForBotTextToContainAnyOf("?", CHAT_BOT_DONE_MESSAGE)
    }

    @Then("the chatbot has asked if I would like to add a comment")
    fun requireBotQuestionToAddAComment() {
        waitForBotText(WOULD_YOU_LIKE, ADD_A_COMMENT)
    }

    @And("the chatbot has asked if I want to provide any (more )reasons")
    fun waitForBotQuestionToProvideReasons() {
        waitForBotTextToContainAnyOf(REASON, SUGGESTION)
    }

    fun waitForBotSuggestions() {
        // Require the suggestions to be newer than the user's last message so
        // that a list left over from an earlier rule session in the same
        // conversation cannot satisfy the wait (chained-rule scenarios).
        awaitBotResponse("suggested conditions for the latest request") {
            chatPO().suggestionsAreForLatestRequest() &&
                    chatPO().mostRecentSuggestionRowContainsTerms(listOf("1."))
        }
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
        await().atMost(ofSeconds(90)).until {
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

    @Then("the chatbot response contains the following terms:")
    fun requireChatbotResponseToContain(terms: DataTable) {
        awaitBotResponse("a bot message containing all of ${terms.asList()}") {
            chatPO().mostRecentBotRowContainsTerms(terms.asList())
        }
    }

    fun waitForBotText(vararg terms: String) {
        awaitBotResponse("a bot message containing all of ${terms.toList()}") {
            chatPO().mostRecentBotRowContainsTerms(terms.toList())
        }
    }

    fun waitForBotTextToContainAnyOf(vararg terms: String) {
        awaitBotResponse("a bot message containing any of ${terms.toList()}") {
            chatPO().mostRecentBotRowContainsAnyOfTheTerms(terms.toList())
        }
    }

    fun waitForSuggestionText(vararg terms: String) {
        awaitBotResponse("a suggestion containing all of ${terms.toList()}") {
            chatPO().mostRecentSuggestionRowContainsTerms(terms.toList())
        }
    }

    /**
     * Wait for [condition], but give up as soon as the bot reports a failure it
     * cannot recover from (the AI being unavailable, or a server error).
     *
     * Without this, a transient LLM failure - e.g. a Gemini call that hangs and
     * is abandoned after its own 90s timeout - leaves a message in the chat that
     * can never satisfy the wait. The wait then burns its full budget and
     * reports only that a lambda "was not fulfilled", which points at the
     * feature under test rather than at the real cause. [expectation] names what
     * was being waited for so the failure says so directly.
     */
    private fun awaitBotResponse(expectation: String, condition: () -> Boolean) {
        try {
            await().atMost(ofSeconds(90)).until {
                chatPO().terminalFailureText() != null || condition()
            }
        } catch (e: ConditionTimeoutException) {
            throw AssertionError("Timed out after 90s waiting for $expectation.", e)
        }
        val failure = chatPO().terminalFailureText()
        if (failure != null && !condition()) {
            throw AssertionError(
                "The chatbot reported a failure it cannot recover from while waiting for " +
                        "$expectation. Bot said: \"$failure\". This is an AI/server failure, not a " +
                        "failure of the behaviour under test - see the saved server.log for the cause."
            )
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
        // Wait for the bot to respond to the final reason before returning,
        // so the next step doesn't race the server/LLM while the chat input
        // is still disabled (e.g. a slow Gemini call on "Sun is hot").
        if (reasons.isNotEmpty()) {
            waitForBotResponseToReason(previousSuggestionCount, messageCountAfterSend)
        }
    }

    /**
     * Enter a condition into a rule session that is already under way. Unlike [provideTheseReasons]
     * this does not require the suggestion list to be newer than the user's most recent message,
     * because the user may have said something else since the suggestions appeared (e.g. asking to
     * see the next cornerstone case).
     */
    fun addConditionToCurrentRuleSession(condition: String) {
        await().atMost(ofSeconds(90)).until {
            chatPO().numberOfSuggestionRows() > 0
        }
        val suggestionsBefore = chatPO().numberOfSuggestionRows()
        enterChatTextAndSend(condition)
        waitForBotResponseToReason(suggestionsBefore, chatPO().numberOfChatMessages())
    }

    fun waitForBotResponseToReason(previousSuggestionCount: Int, messageCountAfterSend: Int) {
        await().atMost(ofSeconds(90)).until {
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
        addCommentWithoutConfirmation(comment)
        waitForBotSuggestions()
        decline()
        waitForBotQuestionToAllowReportChangeToCornerstoneThenConfirm()
        waitForBotToSayDone()
    }

    @And("I request that the comment {string} be added")
    fun requestCommentBeAdded(comment: String) {
        waitForBotQuestionOrCompletedAction()
        // The model is instructed to ask for confirmation when the comment
        // contains a variable, and occasionally asks for other comments too;
        // addCommentThenConfirm confirms only if the model actually asks.
        addCommentThenConfirm(comment)
    }

    @And("I request that the comment with variable(s) {string} be added")
    fun requestCommentWithVariableBeAdded(comment: String) {
        waitForBotQuestionOrCompletedAction()
        // The model is instructed to ask for confirmation when the comment
        // contains a variable, and occasionally asks for other comments too;
        // addCommentThenConfirm confirms only if the model actually asks.
        addCommentWithoutConfirmation(comment)
    }

    @Then("the bot should ask which attribute to use for the placeholder")
    fun requireBotToAskWhichAttributeToUseForPlaceholder() {
        waitForBotText(WHICH_ATTRIBUTE)
    }

    @And("I request that the comment {string} be added without being prompted")
    fun requestCommentBeAddedWithoutPrompt(comment: String) {
        addCommentWithoutConfirmation(comment)
    }

    fun addCommentWithoutConfirmation(comment: String) {
        enterChatTextAndSend("Add the comment: \"$comment\"")
    }

    fun addCommentThenConfirm(comment: String) {
        addCommentWithoutConfirmation(comment)
        // The model is instructed to ask for confirmation when a comment contains a variable, but it
        // occasionally proceeds straight to the rule session (showing suggestions). Only confirm if it
        // actually asks, otherwise the "yes" arrives after the suggestions and is misread as a condition.
        // Detect the suggestion list directly rather than relying on the model's exact wording, and
        // require it to be newer than the add-comment request so a list left over from an earlier rule
        // session cannot satisfy the wait.
        await().atMost(ofSeconds(60)).until {
            chatPO().mostRecentBotRowContainsTerms(listOf(CONFIRM)) || chatPO().suggestionsAreForLatestRequest()
        }
        if (chatPO().mostRecentBotRowContainsTerms(listOf(CONFIRM))) {
            confirm()
        }
    }

    @And("I request that the comment be removed")
    fun requestCommentBeRemoved() {
        waitForBotQuestion()
        enterChatTextAndSend("Remove the comment")
        waitForBotRequestForConfirmationAndConfirm()
    }

    @And("I request that the only comment be removed")
    fun requestOnlyCommentBeRemovedWithoutConfirmation() {
        waitForBotQuestion()
        enterChatTextAndSend("Remove the comment")
    }

    @And("I request that the derived value {string} be removed")
    fun requestDerivedAttributeBeRemoved(attributeName: String) {
        waitForBotQuestion()
        enterChatTextAndSend("Remove derived attribute $attributeName")
    }

    @And("I request that the derived attribute {string} be added with (formula )(value ){string}")
    fun requestDerivedAttributeBeAdded(attributeName: String, formula: String) {
        waitForBotQuestionOrCompletedAction()
        enterChatTextAndSend("Add derived attribute $attributeName with formula $formula")
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

    @When("I ask to see the suggestions again")
    fun askToSeeSuggestedConditionsAgain() {
        enterChatTextAndSend("Please show the suggestions again")
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
        waitForBotToBeReadyForCornerstoneNavigation()
        enterChatTextAndSend("show me the next cornerstone case")
    }

    @When("I ask the chatbot to show the previous cornerstone case")
    fun askToShowPreviousCornerstoneCase() {
        waitForBotToBeReadyForCornerstoneNavigation()
        enterChatTextAndSend("show me the previous cornerstone case")
    }

    /**
     * Cornerstone navigation can be requested either when the bot has asked a question
     * or while it is offering suggested conditions in a rule session (in which case its
     * most recent message is not a question).
     */
    fun waitForBotToBeReadyForCornerstoneNavigation() {
        waitForBotTextToContainAnyOf("?", SUGGESTION, "1.")
    }

    @Then("the chatbot has mentioned the cornerstone case {string}")
    fun waitForBotToMentionCornerstoneCase(name: String) {
        waitForBotText(name)
    }

    @When("I ask what capabilities are available")
    fun askWhatCapabilitiesAreAvailable() {
        enterChatTextAndSend("What can you help me with? List the things you can do.")
    }


    @Then("the chat should explain that the condition would create a cycle involving the following terms:")
    fun chatExplainsCycle(terms: DataTable) {
        waitForBotText(*terms.asList().toTypedArray())
        waitForBotTextToContainAnyOf("cycle", "depend on itself")
    }

    @Then("the chat should explain that the name {string} already exists")
    fun chatExplainsNameAlreadyExists(name: String) {
        waitForBotText(name)
        waitForBotTextToContainAnyOf("already exists", "already used", "already given", "replace")
    }

    @When("I request that the attribute {string} be renamed to {string}")
    fun requestAttributeBeRenamed(currentName: String, newName: String) {
        waitForBotQuestionOrCompletedAction()
        enterChatTextAndSend("Rename the attribute \"$currentName\" to \"$newName\"")
    }

    @When("I request that the comment just added be renamed to {string}")
    fun requestCommentJustAddedBeRenamed(newName: String) {
        // The name of the comment is chosen by the model, so the request refers to
        // the comment rather than naming it: the model knows the name, having just
        // been told it by the system.
        enterChatTextAndSend("Rename that comment to \"$newName\"")
    }

    @Then("the chatbot tells me the name of the comment and that it can be renamed")
    fun waitForBotToNameTheComment() {
        waitForBotText(COMMENT_IS_NAMED, CAN_BE_RENAMED)
    }

    @Then("the chatbot does not tell me the name of the comment")
    fun requireBotNotToNameTheComment() {
        chatPO().mostRecentBotRowDoesNotContainTheTerm(COMMENT_IS_NAMED)
    }

    @Then("the chatbot confirms that {string} has been renamed to {string}")
    fun waitForBotToConfirmRename(currentName: String, newName: String) {
        waitForBotText(RENAMED, currentName, newName)
    }

    @Then("the chatbot confirms a rename to {string}")
    fun waitForBotToConfirmRenameTo(newName: String) {
        waitForBotText(RENAMED, newName)
    }

    @Then("the chatbot explains that the attribute {string} cannot be renamed")
    fun waitForBotToRefuseRename(name: String) {
        waitForBotText(name, CANNOT_BE_RENAMED)
    }

    @Then("the capabilities shown include:")
    fun capabilitiesShownInclude(dataTable: DataTable) {
        waitForBotText(*dataTable.asList().toTypedArray())
    }

    @Then("the chatbot mentions that a case value can be inserted into a comment using braces")
    fun waitForBotToMentionCommentVariableTip() {
        await().atMost(ofSeconds(90)).until {
            chatPO().mostRecentTipRowContainsTerms(listOf(COMMENT_VARIABLE_TIP_KEYWORD))
        }
    }

    @Then("the chatbot has mentioned the comment variable facility exactly once")
    fun requireCommentVariableTipShownExactlyOnce() {
        chatPO().numberOfTipMessages() shouldBe 1
    }

    @Then("the chatbot does not mention the comment variable facility")
    fun requireCommentVariableTipNotShown() {
        chatPO().numberOfTipMessages() shouldBe 0
    }

}