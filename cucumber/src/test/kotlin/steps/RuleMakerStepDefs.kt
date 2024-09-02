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
    fun startRuleToAddNewComment(comment: String) {
        with(interpretationViewPO()) {
            clickChangeInterpretationButton()
            clickAddCommentMenu()
            setAddCommentTextAndClickOK(comment)
        }
    }
    fun startRuleToAddExistingComment(comment: String) {
        with(interpretationViewPO()) {
            clickChangeInterpretationButton()
            clickAddCommentMenu()
            selectExistingCommentToAddClickOK(comment)
        }
    }

    @And("I start to build a rule to add the comment {string} for case {word}")
    fun startToBuildARuleToAddTheCommentForCase(comment: String, caseName: String) {
        caseListPO().select(caseName)
        startRuleToAddNewComment(comment)
    }

    @And("I build a rule to add the comment {string} for case {word}")
    fun buildARuleToAddCommentForCase(comment: String, caseName: String) {
        caseListPO().select(caseName)
        buildRuleToAddNewComment(comment)
    }

    @And("I build another rule to append the comment {string}")
    fun buildAnotherRuleToAppendTheComment(comment: String) {
        buildRuleToAddNewComment(comment)
    }

    @And("I build a rule to add the comment {string} with the condition {string}")
    fun buildARuleToAddCommentWithCondition(comment: String, condition: String) {
        startRuleToAddNewComment(comment)
        with(ruleMakerPO()) {
            clickConditionWithText(condition)
            clickDoneButton()
        }
    }

    @And("I build a rule to add the comment {string}")
    fun buildRuleToAddNewComment(comment: String) {
        startRuleToAddNewComment(comment)
        completeRule()
    }

    @When("I build a rule to add the comment {string} with conditions")
    fun buildRuleToAddCommentWithConditions(comment: String, conditions: DataTable) {
        startRuleToAddNewComment(comment)
        addConditionsAndFinishRule(conditions)
    }

    @When("^I build a rule to add the long comment")
    fun buildRuleToAddLongCommentWithConditions(comment: String) {
        startRuleToAddNewComment(comment)
//        addConditionsAndFinishRule(conditions)
    }

    @And("I build a rule to add the existing comment {string}")
    fun buildRuleToAddExistingComment(comment: String) {
        startRuleToAddExistingComment(comment)
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

    @When("I build a rule to remove the comment {string} with conditions")
    fun buildARuleToRemoveCommentWithConditions(comment: String, conditions: DataTable) {
        startRuleToRemoveComment(comment)
        addConditionsAndFinishRule(conditions)
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
    fun buildARuleToReplaceTheCommentWithCondition(toBeReplaced: String, replacement: String, condition: String) {
        startRuleToReplaceComment(toBeReplaced, replacement)
        completeRuleWithCondition(condition)
    }

    @When("I build a rule to replace the comment {string} by {string} with the conditions")
    fun buildARuleToReplaceTheCommentWithConditionsx(toBeReplaced: String, replacement: String, conditions: DataTable) {
        startRuleToReplaceComment(toBeReplaced, replacement)
        addConditionsAndFinishRule(conditions)
    }

    @When("I click the suggested condition {string}")
    fun clickTheSuggestedCondition(text: String) {
        ruleMakerPO().clickConditionWithText(text)
    }

    @And("the suggested conditions should not contain:")
    fun theSuggestedConditionsShouldNotContain( dataTable: DataTable) {
        val absentConditions = dataTable.asList().toSet()
        ruleMakerPO().requireAvailableConditionsDoesNotContain(absentConditions)
    }

    @And("the suggested conditions should contain:")
    fun theSuggestedConditionsShouldContain( dataTable: DataTable) {
        val absentConditions = dataTable.asList().toSet()
        ruleMakerPO().requireAvailableConditionsContains(absentConditions)
    }

    @And("the selected conditions should be:")
    fun theSelectedConditionsShouldBe(dataTable: DataTable){
        ruleMakerPO().requireSelectedConditions(dataTable.asList())
    }

    @When("I set the editable value to be {string} and click ok")
    fun setTheEditableValueToBe( text: String) {
        ruleMakerPO().setEditableValue(text)
    }

    @And("the selected conditions should not contain:")
    fun theSelectedConditionsShouldNotContain( dataTable: DataTable) {
        val conditions = dataTable.asList().toSet()
        ruleMakerPO().requireSelectedConditionsDoesNotContain(conditions)
    }

    @And("the selected conditions should contain:")
    fun theSelectedConditionsShouldContain( dataTable: DataTable) {
        val conditions = dataTable.asList().toSet()
        ruleMakerPO().requireSelectedConditionsContains(conditions)
    }

    @And("I build a rule to replace the comment {string} with the comment {string} with conditions")
    fun buildARuleToReplaceTheCommentWithTheCommentWithConditions(toBeReplaced: String, replacement: String, conditions: DataTable ) {
        pause(100)
        startRuleToReplaceComment(toBeReplaced, replacement)
        pause(100)
        addConditionsAndFinishRule(conditions)
    }
}

fun startRuleBuildingSessionToAddComment(comment: String) {
    with(interpretationViewPO()) {
        clickChangeInterpretationButton()
        clickAddCommentMenu()
        setAddCommentTextAndClickOK(comment)
    }
}

fun startRuleToReplaceComment(toBeReplaced: String, replacement: String) {
    with(interpretationViewPO()) {
        clickChangeInterpretationButton()
        clickReplaceCommentMenu()
        pause(100) //TODO remove
        selectCommentToReplaceAndEnterItsReplacementAndClickOK(toBeReplaced, replacement)
        pause(100) //TODO remove
    }
}
fun addConditionsAndFinishRule(dataTable: DataTable) {
    dataTable.asList().forEach { condition ->
        pause(100)
        ruleMakerPO().clickConditionWithText(condition)
    }
    pause(100)
    ruleMakerPO().clickDoneButton()
}

fun startRuleToRemoveComment(comment: String) {
    with(interpretationViewPO()) {
        clickChangeInterpretationButton()
        clickRemoveCommentMenu()
        selectCommentToRemoveAndClickOK(comment)
    }
}

fun completeRuleWithCondition(condition: String) {
    with(ruleMakerPO()) {
        clickConditionWithText(condition)
        clickDoneButton()
    }
}
