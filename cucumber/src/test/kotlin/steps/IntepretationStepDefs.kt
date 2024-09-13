package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.And

class IntepretationStepDefs {
    @And("move pointer to the comment {string}")
    fun movePointerToComment(text: String) {
        interpretationViewPO().movePointerToComment(text)
    }

    @And("the condition(s) showing in the interpretation view (are)(is):")
    fun requireConditionsToBeShowing(conditions: DataTable) {
        interpretationViewPO().waitForConditionsToBeShowing(conditions.asList())
    }
}