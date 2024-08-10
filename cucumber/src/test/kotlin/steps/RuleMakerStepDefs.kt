package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java8.En
import io.rippledown.integration.pause

class RuleMakerStepDefs : En {
    private fun addConditionsAndFinishRule(dataTable: DataTable) {
        dataTable.asList().forEach { condition ->
            pause(1000)
            ruleMakerPO().clickConditionWithText(condition)
        }
        pause(1000)
        ruleMakerPO().clickDoneButton()
    }

    private fun startRuleBuildingSessionToAddComment(comment: String) {
        with(interpretationViewPO()) {
            clickChangeInterpretationButton()
            clickAddCommentMenu()
            setAddCommentTextAndClickOK(comment)
        }
    }

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

        When("I build a rule to replace the comment {string} with the comment {string} with conditions") {toGo: String, replacement: String, conditions: DataTable ->
            pause(1000)
            val currentInterpretation = interpretationViewPO().interpretationText()
            println("------- replacing comment '$toGo' with '$replacement' ----------")
            println("current interp: '$currentInterpretation'")
            val withCommentReplaced = currentInterpretation.replace(toGo, replacement)
            println("with comment replaced = '$withCommentReplaced'")
            interpretationViewPO().setVerifiedText(withCommentReplaced)
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

        When("I click the suggested condition {string}") { text: String ->
            ruleMakerPO().clickConditionWithText(text)
        }

        When("I set the editable value to be {string} and click ok") { text: String ->
            ruleMakerPO().setEditableValue(text)
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

        Then("the suggested conditions should not contain:") { dataTable: DataTable ->
            val absentConditions = dataTable.asList().toSet()
            ruleMakerPO().requireAvailableConditionsDoesNotContain(absentConditions)
        }

        Then("the selected conditions should be:") { dataTable: DataTable ->
            ruleMakerPO().requireSelectedConditions(dataTable.asList())
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