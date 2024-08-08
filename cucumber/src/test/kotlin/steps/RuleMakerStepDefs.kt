package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.And
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.rippledown.integration.pause

class RuleMakerStepDefs {
    @When("I build a rule for the change on row {int}")
    fun IBuildARuleForTheChangeOnRowInt(row: Int) {
        interpretationViewPO().selectDifferencesTab()
        interpretationViewPO().buildRule(row)
    }

    @When("I build a rule for this change")
    fun IBuildARuleForThisChange() {
        interpretationViewPO().selectDifferencesTab()
        interpretationViewPO().buildRule(0)
    }

    @When("I complete the rule")
    fun ICompleteTheRule() {
        ruleMakerPO().clickDoneButton()
    }

    @When("(I )cancel the rule")
    fun ICancelTheRule() {
        ruleMakerPO().clickCancelButton()
    }

    @When("I start to build a rule for the change on row {int}")
    fun IStartToBuildARuleForTheChangeOnRowInt(row: Int) {
        interpretationViewPO().selectDifferencesTab()
        interpretationViewPO().clickBuildIconOnRow(row)
    }

    @When("I select the {word} condition")
    fun ISelectTheWordCondition(position: String) {
        when (position) {
            "first" -> ruleMakerPO().clickAvailableCondition(0)
            "second" -> ruleMakerPO().clickAvailableCondition(1)
            "third" -> ruleMakerPO().clickAvailableCondition(2)
        }
    }

    @When("I add the condition {string}")
    fun IAddTheConditionString(text: String) {
        ruleMakerPO().clickConditionWithText(text)
    }

    @When("I remove the condition {string}")
    fun IRemoveTheConditionString(text: String) {
        ruleMakerPO().removeConditionWithText(text)
    }

    @Then("the conditions showing should be:")
    fun theConditionsShowingShouldBe(dataTable: DataTable) {
        val expectedConditions = dataTable.asList()
        ruleMakerPO().requireAvailableConditions(expectedConditions)
    }

    @And("I build a rule to add the comment {string} with the condition {string}")
    fun IBuildARuleToAddTheCommentStringWithTheConditionString(comment: String, condition: String) {
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

    @And("I build a rule to add the comment {string} for case {word}")
    fun IBuildARuleToAddTheCommentStringForCaseWord(comment: String, caseName: String) {
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

    @And("I build another rule to append the comment {string}")
    fun IBuildAnotherRuleToAppendTheCommentString(comment: String) {
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

    @And("I start to build a rule to add the comment {string} for case {word}")
    fun IStartToBuildARuleToAddTheCommentStringForCaseWord(comment: String, caseName: String) {
        caseListPO().select(caseName)

        with(interpretationViewPO()) {
            setVerifiedText(comment)
            selectDifferencesTab()
            clickBuildIconOnRow(0)
        }

    }

    @And("I build a rule to add a comment {string}")
    fun IBuildARuleToAddACommentString(comment: String) {
        with(interpretationViewPO()) {
            clickChangeInterpretationButton()
            clickAddCommentMenu()
            setAddCommentTextAndClickOK(comment)
        }
        with(ruleMakerPO()) {
            clickDoneButton()
        }

    }

    @When("I build a rule to remove the comment {string}")
    fun IBuildARuleToRemoveTheCommentString(comment: String) {
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

    @When("I build a rule to replace the comment {string} with {string}")
    fun IBuildARuleToReplaceTheCommentStringWithString(toBeReplaced: String, replacement: String) {
        with(interpretationViewPO()) {
            clickChangeInterpretationButton()
            clickReplaceCommentMenu()
            pause(1000) //TODO remove
            selectCommentToReplaceAndEnterItsReplacementAndClickOK(toBeReplaced, replacement)
            pause(1000) //TODO remove
        }
        with(ruleMakerPO()) {
            clickDoneButton()
        }
    }

}