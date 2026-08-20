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

    @And("the comment {string} should be shown with the name {string}")
    fun requireCommentToBeNamed(comment: String, name: String) {
        interpretationViewPO().waitForCommentToBeNamed(comment, name)
    }

    fun requireConditionsToBeShowing(conditions: DataTable) {
        interpretationViewPO().waitForConditionsToBeShowing(conditions.asList())
    }
}