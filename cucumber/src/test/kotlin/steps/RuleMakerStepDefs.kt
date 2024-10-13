package steps

import androidx.compose.ui.awt.ComposeDialog
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.And
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.interpretation.OK_BUTTON_FOR_REMOVE_COMMENT
import io.rippledown.constants.interpretation.OK_BUTTON_FOR_REPLACE_COMMENT
import io.rippledown.constants.interpretation.REMOVE_COMMENT_TEXT_FIELD
import io.rippledown.constants.interpretation.REPLACED_COMMENT_TEXT_FIELD
import io.rippledown.integration.pause
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findComposeDialogThatIsShowing
import io.rippledown.integration.waitUntilAsserted
import org.assertj.swing.edt.GuiActionRunner.execute
import javax.accessibility.AccessibleState
import javax.accessibility.AccessibleStateSet

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

    @Then("the conditions showing should include:")
    fun theConditionsShowingShouldInclude(dataTable: DataTable) {
        val expectedConditions = dataTable.asList().toSet()
        ruleMakerPO().requireAvailableConditionsContains(expectedConditions)
    }

    @And("I start to build a rule to add the comment {string}")
    fun startRuleToAddNewComment(comment: String) {
        with(interpretationViewPO()) {
            clickChangeInterpretationButton()
            clickAddCommentMenu()
            setAddCommentTextAndClickOK(comment)
        }
    }

    @And("I start to build a rule to remove the comment {string}")
    fun startRuleToRemoveComment(comment: String) {
        with(interpretationViewPO()) {
            clickChangeInterpretationButton()
            clickRemoveCommentMenu()
            selectCommentToRemoveAndClickOK(comment)
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

    @When("I start to build a rule to remove a comment")
    fun startRuleToRemoveAComment() {
        with(interpretationViewPO()) {
            clickChangeInterpretationButton()
            clickRemoveCommentMenu()
        }
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

    @And("I enter {string} as the filter to select a comment to remove")
    fun enterFilterTextIntoTheRemoveCommentToRemove(filterText: String) {
        enterFilterText(filterText, REMOVE_COMMENT_TEXT_FIELD)
    }

    @And("I enter {string} as the filter to select a comment to replace")
    fun enterFilterTextIntoTheRemoveCommentToReplace(filterText: String) {
        enterFilterText(filterText, REPLACED_COMMENT_TEXT_FIELD)
    }

    private fun enterFilterText(filterText: String, contentDescriptionForTextField: String) {
        waitUntilAsserted {
            execute<ComposeDialog> { findComposeDialogThatIsShowing() } shouldNotBe null
        }
        val dialog = execute<ComposeDialog> { findComposeDialogThatIsShowing() }
        with(dialog.accessibleContext) {
            //Enter the filter text
            execute { find(contentDescriptionForTextField)!!.accessibleEditableText.setTextContents(filterText) }
        }
    }

    @Then("the OK button to start the rule to remove the comment should be disabled")
    fun theOKButtonToStartTheRuleToRemoveTheCommentShouldBeDisabled() {
        requireButtonToBeDisabled(OK_BUTTON_FOR_REMOVE_COMMENT)
    }

    @Then("the OK button to start the rule to replace the comment should be disabled")
    fun theOKButtonToStartTheRuleToReplaceTheCommentShouldBeDisabled() {
        requireButtonToBeDisabled(OK_BUTTON_FOR_REPLACE_COMMENT)
    }

    private fun requireButtonToBeDisabled(contentDescriptionForButton: String) {
        waitUntilAsserted {
            execute<ComposeDialog> { findComposeDialogThatIsShowing() } shouldNotBe null
        }
        val dialog = execute<ComposeDialog> { findComposeDialogThatIsShowing() }
        with(dialog.accessibleContext) {
            val accessibleStateSet =
                execute<AccessibleStateSet> { find(contentDescriptionForButton)!!.accessibleStateSet }
            accessibleStateSet.contains(AccessibleState.ENABLED) shouldBe false
        }
    }

    @When("I start to build a rule to replace a comment")
    fun startRuleToReplaceAComment() {
        with(interpretationViewPO()) {
            clickChangeInterpretationButton()
            clickReplaceCommentMenu()
        }
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
    fun theSuggestedConditionsShouldNotContain(dataTable: DataTable) {
        val absentConditions = dataTable.asList().toSet()
        ruleMakerPO().requireAvailableConditionsDoesNotContain(absentConditions)
    }

    @And("the suggested conditions should contain:")
    fun theSuggestedConditionsShouldContain(dataTable: DataTable) {
        val absentConditions = dataTable.asList().toSet()
        ruleMakerPO().requireAvailableConditionsContains(absentConditions)
    }

    @And("the selected conditions should be:")
    fun theSelectedConditionsShouldBe(dataTable: DataTable) {
        ruleMakerPO().requireSelectedConditions(dataTable.asList())
    }

    @When("I set the editable value to be {string} and click ok")
    fun setTheEditableValueToBe(text: String) {
        pause(100)
        ruleMakerPO().setEditableValue(text)
        pause(100)
    }

    @And("the selected conditions should not contain:")
    fun theSelectedConditionsShouldNotContain(dataTable: DataTable) {
        val conditions = dataTable.asList().toSet()
        ruleMakerPO().requireSelectedConditionsDoesNotContain(conditions)
    }

    @And("the selected conditions should contain:")
    fun theSelectedConditionsShouldContain(dataTable: DataTable) {
        val conditions = dataTable.asList().toSet()
        ruleMakerPO().requireSelectedConditionsContains(conditions)
    }

    @And("I build a rule to replace the comment {string} with the comment {string} with conditions")
    fun buildARuleToReplaceTheCommentWithTheCommentWithConditions(
        toBeReplaced: String,
        replacement: String,
        conditions: DataTable
    ) {
        pause(100)
        startRuleToReplaceComment(toBeReplaced, replacement)
        pause(100)
        addConditionsAndFinishRule(conditions)
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
        println("Expression: $expression")
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
    if (dataTable.width() == 1) {
        addNonEditableConditionsAndFinishRule(dataTable.asList())
    } else {
        addConditionsThatMayBeEditable(dataTable.asLists())
    }
}

fun addConditionsThatMayBeEditable(conditionsWithHints: List<List<String>>) {
    conditionsWithHints.forEach { conditionWithHints ->
        pause(100)
        if (conditionWithHints[1] != null && conditionWithHints[1].isNotBlank()) {
            ruleMakerPO().clickConditionStartingWithText(conditionWithHints[1])
            pause(200)
            ruleMakerPO().setEditableValue(conditionWithHints[2])
            pause(100)
            ruleMakerPO().requireSelectedConditionsContains(conditionWithHints[0])
        } else {
            ruleMakerPO().clickConditionStartingWithText(conditionWithHints[0])
            pause(100)
        }
    }
    pause(100)
    ruleMakerPO().clickDoneButton()
}

fun addNonEditableConditionsAndFinishRule(conditions: List<String>) {
    conditions.forEach { condition ->
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
