package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Then

class BackdoorTooltipStepDefs {
    @Then("the backdoor tooltip for the comment {string} has conditions:")
    fun requireConditions(comment: String, conditions: DataTable) {
        commentTooltipPO().waitForConditions(comment, conditions.asList())
    }

    @Then("the backdoor tooltip for the comment {string} has phrases:")
    fun requirePhrases(comment: String, phrases: DataTable) {
        commentTooltipPO().waitForPhrases(comment, phrases.asList())
    }

    @Then("the backdoor tooltip for the comment {string} has no conditions")
    fun requireNoConditions(comment: String) {
        commentTooltipPO().waitForConditions(comment, emptyList())
    }
}
