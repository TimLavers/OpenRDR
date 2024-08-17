package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.And
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.rippledown.integration.pause

class RuleMakerStepDefs {

    @When("I complete the rule")
    fun completeRule() {
        ruleMakerPO().clickDoneButton()
    }

    @When("(I )cancel the rule")
    fun cancelTheRule() {
        ruleMakerPO().clickCancelButton()
    }

    @When("I select the {word} condition")
    fun selectTheCondition(position: String) {
        when (position) {
            "first" -> ruleMakerPO().clickAvailableCondition(0)
            "second" -> ruleMakerPO().clickAvailableCondition(1)
            "third" -> ruleMakerPO().clickAvailableCondition(2)
        }
    }

    @When("I add the condition {string}")
    fun addTheCondition(text: String) {
        ruleMakerPO().clickConditionWithText(text)
    }

    @When("I remove the condition {string}")
    fun removeTheConditionString(text: String) {
        ruleMakerPO().removeConditionWithText(text)
    }

    @Then("the conditions showing should be:")
    fun theConditionsShowingShouldBe(dataTable: DataTable) {
        val expectedConditions = dataTable.asList()
        ruleMakerPO().requireAvailableConditions(expectedConditions)
    }

    @And("I start to build a rule to add the comment {string}")
    fun startRuleToAddComment(comment: String) {
        with(interpretationViewPO()) {
            clickChangeInterpretationButton()
            clickAddCommentMenu()
            setAddCommentTextAndClickOK(comment)
        }
    }

    @And("I start to build a rule to add the comment {string} for case {word}")
    fun startToBuildARuleToAddTheCommentForCase(comment: String, caseName: String) {
        caseListPO().select(caseName)
        startRuleToAddComment(comment)
    }

    @And("I build a rule to add the comment {string} for case {word}")
    fun buildARuleToAddCommentForCase(comment: String, caseName: String) {
        caseListPO().select(caseName)
        buildRuleToAddComment(comment)
    }

    @And("I build another rule to append the comment {string}")
    fun buildAnotherRuleToAppendTheComment(comment: String) {
        buildRuleToAddComment(comment)
    }

    @And("I build a rule to add the comment {string} with the condition {string}")
    fun buildARuleToAddCommentWithCondition(comment: String, condition: String) {
        startRuleToAddComment(comment)
        with(ruleMakerPO()) {
            clickConditionWithText(condition)
            clickDoneButton()
        }
    }

    @And("I build a rule to add a comment {string}")
    fun buildRuleToAddComment(comment: String) {
        startRuleToAddComment(comment)
        completeRule()
    }

    @When("I build a rule to remove the comment {string}")
    fun buildARuleToRemoveTheComment(comment: String) {
        startRuleToRemoveComment(comment)
        completeRule()
    }

    @When("I build a rule to remove the comment {string} with the condition {string}")
    fun buildARuleToRemoveTheCommentWithCondition(comment: String, condition: String) {
        startRuleToRemoveComment(comment)
        completeRuleWithCondition(condition)
    }

    @When("I start to build a rule to replace the comment {string} by {string}")
    fun startRuleToReplaceCommentBy(toBeReplaced: String, replacement: String) {
        startRuleToReplaceComment(toBeReplaced, replacement)
    }

    @When("I build a rule to replace the comment {string} by {string}")
    fun buildARuleToReplaceTheComment(toBeReplaced: String, replacement: String) {
        startRuleToReplaceComment(toBeReplaced, replacement)
        completeRule()
    }

    @When("I build a rule to replace the comment {string} by {string} with the condition {string}")
    fun buildARuleToReplaceTheCommentWithConditions(toBeReplaced: String, replacement: String, condition: String) {
        startRuleToReplaceComment(toBeReplaced, replacement)
        completeRuleWithCondition(condition)
    }
}

fun startRuleToReplaceComment(toBeReplaced: String, replacement: String) {
    with(interpretationViewPO()) {
        clickChangeInterpretationButton()
        clickReplaceCommentMenu()
        pause(1000) //TODO remove
        selectCommentToReplaceAndEnterItsReplacementAndClickOK(toBeReplaced, replacement)
        pause(1000) //TODO remove
    }
}

fun startRuleToRemoveComment(comment: String) {
    with(interpretationViewPO()) {
        clickChangeInterpretationButton()
        clickRemoveCommentMenu()
        pause(1000) //TODO remove
        selectCommentToRemoveAndClickOK(comment)
        pause(1000) //TODO remove
    }
}

fun completeRuleWithCondition(condition: String) {
    with(ruleMakerPO()) {
        clickConditionWithText(condition)
        clickDoneButton()
    }
}
