package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.And

class IntepretationStepDefs {
    @And("move pointer to the comment {string}")
    fun movePointerToComment(text: String) {
        interpretationViewPO().movePointerToComment(text)
    }

    @And("the condition(s) showing for the comment {string} (is)(are):")
    fun requireConditionsForComment(comment: String, conditions: DataTable) {
        interpretationViewPO().waitForConditionsForComment(comment, conditions.asList())
    }

    @And("no conditions should be showing for the comment {string}")
    fun requireNoConditionsForComment(comment: String) {
        interpretationViewPO().movePointerToComment(comment)
        interpretationViewPO().requireNoConditionsToBeShowing()
    }

    fun requireConditionsToBeShowing(conditions: DataTable) {
        interpretationViewPO().waitForConditionsToBeShowing(conditions.asList())
    }
}