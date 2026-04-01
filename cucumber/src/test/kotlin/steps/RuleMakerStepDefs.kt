package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.And
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.kotest.matchers.collections.shouldContainAll
import io.rippledown.integration.pause
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds
import java.util.concurrent.TimeUnit.SECONDS

class RuleMakerStepDefs(private val chatDefs: ChatDefs) {

    @And("I build a rule to add the comment {string}")
    fun buildRuleToAddNewComment(comment: String) {
        with(chatDefs) {
            requestCommentBeAdded(comment)
            completeRule()
        }
    }

    @And("I build a rule to add another comment for the same case {string}")
    fun buildRuleToAddAnotherAnotherCommentForTheSameCase(comment: String) {
        with(chatDefs) {
            addComment(comment)
            completeRule()
        }
    }

    @When("I build a rule to replace that comment by {string}")
    fun replaceCommentWithoutConditions(replacement: String) {
        chatDefs.requestTheOnlyCommentBeReplacedBy(replacement)
        completeRule()
    }

    @When("Replace the comment {string} by {string} with the reasons:")
    fun replaceCommentWithConditions(toBeReplaced: String, replacement: String, conditions: DataTable) {
        chatDefs.requestCommentBeReplacedBy(toBeReplaced, replacement)
        completeRuleWithConditions(conditions)
    }

    fun completeRuleWithConditions(conditions: DataTable) {
        with(chatDefs) {
            provideReasonsThenDeclineToAddMore(conditions)
            waitForBotToSayDone()
        }
    }

    fun completeRule() {
        with(chatDefs) {
            waitForBotSuggestions()
            decline()
            waitForBotToSayDone()
        }
    }

    @When("(I )cancel the rule")
    fun cancelTheRule() {
        chatDefs.enterChatTextAndSend("cancel this rule")
    }

    @When("I select the {word} suggestion")
    fun selectTheCondition(position: String) {
        when (position) {
            "first" -> chatDefs.enterChatTextAndSend("1")
            "second" -> chatDefs.enterChatTextAndSend("2")
            "third" -> chatDefs.enterChatTextAndSend("3")
        }
    }

    @When("I add the condition {string}")
    fun addTheCondition(text: String) {
        chatDefs.provideTheseReasons(listOf(text))
    }

    @Then("the suggestions showing should include:")
    fun theConditionsShowingShouldInclude(dataTable: DataTable) {
        val expectedConditions = dataTable.asList().toSet()
        await().atMost(20, SECONDS).untilAsserted {
            chatPO().suggestionsInMostRecentMessage() shouldContainAll (expectedConditions)
        }
    }

    @And("I start to build a rule to remove the comment {string}")
    fun startRuleToRemoveComment(comment: String) {
        chatDefs.requestCommentBeRemoved(comment)
    }

    @And("I start to build a rule to add the comment {string} for case {word}")
    fun startToBuildARuleToAddTheCommentForCase(comment: String, caseName: String) {
        caseListPO().select(caseName)
        chatDefs.requestCommentBeAdded(comment)
    }

    @And("I build a rule to add the comment {string} for case {word}")
    fun buildARuleToAddCommentForCase(comment: String, caseName: String) {
        caseListPO().select(caseName)
        buildRuleToAddNewComment(comment)
    }

    @And("I build another rule to append the comment {string}")
    fun buildAnotherRuleToAppendTheComment(comment: String) {
        chatDefs.addComment(comment)
        completeRule()
    }

    @When("I build a rule to add the comment {string} with condition(s)")
    fun buildRuleToAddCommentWithConditions(comment: String, conditions: DataTable) {
        chatDefs.requestCommentBeAdded(comment)
        completeRuleWithConditions(conditions)
    }

    @And("I build a rule to add the existing comment {string}")
    fun buildRuleToAddExistingComment(comment: String) {
        chatDefs.requestCommentBeAdded(comment)
        completeRule()
    }


    @When("I build another rule to remove the comment {string}")
    fun buildAnotherRuleToRemoveTheComment(comment: String) {
        chatDefs.removeComment(comment)
        completeRule()
    }

    @When("I build a rule to remove the comment {string} with condition(s)")
    fun buildARuleToRemoveCommentWithConditions(comment: String, conditions: DataTable) {
        chatDefs.requestCommentBeRemoved(comment)
        chatDefs.provideReasonsThenDeclineToAddMore(conditions)
    }

    @When("I start to build a rule to replace the comment {string} by {string}")
    fun startRuleToReplaceCommentBy(toBeReplaced: String, replacement: String) {
        chatDefs.requestCommentBeReplacedBy(toBeReplaced, replacement)
    }

    @When("I try to build a rule to replace the non-existing comment {string}")
    fun startRuleToReplaceNonExistingComment(toBeReplaced: String) {
        with(chatDefs) {
            waitForBotQuestion()
            enterChatTextAndSend("Replace the '$toBeReplaced' comment")
        }
    }

    @When("I request another change to be made to the report")
    fun requestAnotherChangeToBeMadeToTheReport() {
        chatPO().enterChatText("another change to the report please")
        chatPO().clickSend()
    }

    @When("I build a rule to replace the comment {string} by {string} with the condition(s)")
    fun buildARuleToReplaceTheCommentWithConditions(toBeReplaced: String, replacement: String, conditions: DataTable) {
        chatDefs.requestCommentBeReplacedBy(toBeReplaced, replacement)
        chatDefs.provideReasonsThenDeclineToAddMore(conditions)
    }

    @And("the suggested conditions should not contain:")
    fun theSuggestedConditionsShouldNotContain(dataTable: DataTable) {
        val absentConditions = dataTable.asList().toSet()
        absentConditions.forEach {
            chatPO().mostRecentSuggestionRowDoesNotContainsTerm(it)
        }
    }

    @And("the suggested conditions should not contain {string}")
    fun waitForSuggestionsToNotContain(term: String) {
        await().atMost(ofSeconds(10)).until {
            chatPO().mostRecentSuggestionRowDoesNotContainsTerm(term)
        }
    }

    @And("the suggested conditions should contain:")
    fun theSuggestedConditionsShouldContain(dataTable: DataTable) {
        val terms = dataTable.asList()
        await().atMost(ofSeconds(20)).until {
            chatPO().mostRecentSuggestionRowContainsTerms(terms)
        }
    }

    @When("I set the editable value to be {string}")
    fun setTheEditableValueToBe(text: String) {
        pause(100)
        chatPO().mostRecentBotRowContainsTerms(listOf("you selected", "What value"))
        chatPO().enterChatText(text)
        chatPO().clickSend()
    }

    @And("I build a rule to replace the comment {string} with the comment {string} with conditions")
    fun buildARuleToReplaceTheCommentWithTheCommentWithConditions(
        toBeReplaced: String,
        replacement: String,
        conditions: DataTable
    ) {
        chatDefs.requestCommentBeReplacedBy(toBeReplaced, replacement)
        chatDefs.provideReasonsThenDeclineToAddMore(conditions)
    }

    @Then("the message indicating the comment {string} is being added should be shown")
    fun `require message indicating comment is being added`(addedComment: String) {
        ruleMakerPO().requireMessageForAddingComment(addedComment)
    }

    @Then("the message indicating the comment {string} is being removed should be shown")
    fun `require message indicating comment is being removed`(removedComment: String) {
        ruleMakerPO().requireMessageForRemovingComment(removedComment)
    }

    @Then("the message indicating the comment {string} is being replaced by {string} should be shown")
    fun `require message indicating comment is being replaced`(replacedComment: String, replacementComment: String) {
        ruleMakerPO().requireMessageForReplacingComment(replacedComment, replacementComment)
    }

    @Then("I enter the expression {string}")
    fun `enter expression`(expression: String) {
        chatDefs.provideTheseReasons(listOf(expression))
    }

    @Then("the model should respond with a message containing:")
    fun `require alert`(expected: String) {
        chatDefs.waitForBotText(expected)
    }

    @Then("the model should indicate that the expression is not a valid reason")
    fun `require invalid reason response`() {
        chatDefs.waitForBotResponseIndicatingInvalidReason()
    }
}
