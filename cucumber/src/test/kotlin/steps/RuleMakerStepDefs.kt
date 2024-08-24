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

    @And("I build a rule to add the comment {string} with conditions")
    fun buildARuleToAddCommentWithConditions(comment: String, conditions: DataTable) {
        startRuleBuildingSessionToAddComment(comment)
        pause(1000)
        addConditionsAndFinishRule(conditions)
    }

    @And("I build a rule to replace the comment {string} with the comment {string} with conditions")
    fun buildARuleToReplaceTheCommentWithTheCommentWithConditions(toBeReplaced: String, replacement: String, conditions: DataTable ) {
        pause(1000)
        startRuleToReplaceComment(toBeReplaced, replacement)
        pause(10000)
        addConditionsAndFinishRule(conditions)
    }

    @And("I build a rule to remove the comment {string} with conditions")
    fun buildARuleToRemoveTheCommentWithConditions(comment: String, conditions: DataTable) {
        startRuleToRemoveComment(comment)
        pause(10000)
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
        pause(1000) //TODO remove
        selectCommentToReplaceAndEnterItsReplacementAndClickOK(toBeReplaced, replacement)
        pause(1000) //TODO remove
    }
}
fun addConditionsAndFinishRule(dataTable: DataTable) {
    dataTable.asList().forEach { condition ->
        pause(1000)
        ruleMakerPO().clickConditionWithText(condition)
    }
    pause(10000)
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


/*


class RuleMakerStepDefs : En {


    init {
        When("I build a rule to add the comment {string} with conditions") {comment: String, conditions: DataTable ->
            startRuleBuildingSessionToAddComment(comment)
            pause(1000)
            addConditionsAndFinishRule(conditions)
        }

        When("I start building a rule to add the comment {string}") {comment: String ->
            startRuleBuildingSessionToAddComment(comment)
        }

        When("I build a rule to remove the comment {string} with conditions") {comment: String, conditions: DataTable ->
            pause(1000)
            val currentInterpretation = interpretationViewPO().interpretationText()
            println("------- removing comment '$comment' ----------")
            println("current interp: '$currentInterpretation'")
            val withCommentRemoved = currentInterpretation.replace(comment, "")
            println("with comment removed = '$withCommentRemoved'")
            interpretationViewPO().setVerifiedText(withCommentRemoved)
            pause(1000)
            interpretationViewPO().selectDifferencesTab()
            pause(1000)
            interpretationViewPO().clickBuildIconOnRow(0)
            addConditionsAndFinishRule(conditions)
        }


        When("I build a rule for the change on row {int}") { row: Int ->
            interpretationViewPO().selectDifferencesTab()
            interpretationViewPO().buildRule(row)
        }

        When("I build a rule for this change") {
            interpretationViewPO().selectDifferencesTab()
            interpretationViewPO().buildRule(0)
        }

        When("I complete the rule") {
            ruleMakerPO().clickDoneButton()
        }

        When("(I )cancel the rule") {
            ruleMakerPO().clickCancelButton()
        }

        When("I start to build a rule for the change on row {int}") { row: Int ->
            interpretationViewPO().selectDifferencesTab()
            interpretationViewPO().clickBuildIconOnRow(row)
        }

        When("I select the {word} condition") { position: String ->
            when (position) {
                "first" -> ruleMakerPO().clickAvailableCondition(0)
                "second" -> ruleMakerPO().clickAvailableCondition(1)
                "third" -> ruleMakerPO().clickAvailableCondition(2)
            }
        }


        When("I add the condition {string}") { text: String ->
            ruleMakerPO().clickConditionWithText(text)
        }

        When("I remove the condition {string}") { text: String ->
            ruleMakerPO().removeConditionWithText(text)
        }

        Then("the conditions showing should be:") { dataTable: DataTable ->
            val expectedConditions = dataTable.asList()
            ruleMakerPO().requireAvailableConditions(expectedConditions)
        }


        And("I build a rule to add the comment {string} with the condition {string}") { comment: String, condition: String ->
            with(interpretationViewPO()) {
                setVerifiedText(comment)
                selectDifferencesTab()
                clickBuildIconOnRow(0)
            }
            with(ruleMakerPO()) {
                clickConditionWithText(condition)
                clickDoneButton()
            }
        }

        And("I build a rule to add the comment {string} for case {word}") { comment: String, caseName: String ->
            caseListPO().select(caseName)

            with(interpretationViewPO()) {
                selectOriginalTab()
                setVerifiedText(comment)
                selectDifferencesTab()
                clickBuildIconOnRow(0)
            }
            with(ruleMakerPO()) {
                clickDoneButton()
            }
        }

        And("I build another rule to append the comment {string}") { comment: String ->
            with(interpretationViewPO()) {
                selectOriginalTab()
                pause() //TODO remove this
                addVerifiedTextAtEndOfCurrentInterpretation(comment)
                selectDifferencesTab()

                //The first row has the unchanged comment
                //The second row is the new comment
                buildRule(row = 1)
            }
        }

        And("I start to build a rule to add the comment {string} for case {word}") { comment: String, caseName: String ->
            caseListPO().select(caseName)

            with(interpretationViewPO()) {
                setVerifiedText(comment)
                selectDifferencesTab()
                clickBuildIconOnRow(0)
            }
        }

        And("I build a rule to add a comment {string}") { comment: String ->
            startRuleBuildingSessionToAddComment(comment)
            with(ruleMakerPO()) {
                clickDoneButton()
            }
        }
        When("I build a rule to remove the comment {string}") { comment: String ->
            with(interpretationViewPO()) {
                clickChangeInterpretationButton()
                clickRemoveCommentMenu()
                pause(2000) //TODO remove
                selectCommentToRemoveAndClickOK(comment)
                pause(2000) //TODO remove
            }
            with(ruleMakerPO()) {
                clickDoneButton()
            }
        }
        Then("the condition editor is launched and shows") { dataTable: DataTable ->

            TODO("Not yet implemented")
        }
    }
}

 */
