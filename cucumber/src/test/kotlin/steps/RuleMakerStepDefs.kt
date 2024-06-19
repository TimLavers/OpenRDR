package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java8.En
import io.rippledown.integration.pause

class RuleMakerStepDefs : En {
    init {
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

        And("I build a rule to add the comment {string}") { comment: String ->
            with(interpretationViewPO()) {
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



    }
}