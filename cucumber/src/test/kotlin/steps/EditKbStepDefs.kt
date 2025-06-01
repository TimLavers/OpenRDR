package steps

import io.cucumber.datatable.DataTable
import io.cucumber.docstring.DocString
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.kotest.matchers.shouldBe
import io.rippledown.integration.waitUntilAsserted

class EditKbStepDefs {
    @When("I set the KB description to:")
    fun i_set_the_description_to(description: DocString) {
        val descriptionOperator = editCurrentKbControlPO().showDescriptionOperator()
        descriptionOperator.setDescription(description.content)
    }

    @Then("the KB description is:")
    fun the_KBDescriptionIsNow(description: DocString) {
        val expectedText = description.content ?: ""
        val descriptionOperator = editCurrentKbControlPO().showDescriptionOperator()
        descriptionOperator.description() shouldBe expectedText
        descriptionOperator.cancel()
    }

    @When("I undo the last rule")
    fun i_undo_the_last_rule() {
        val operator = editCurrentKbControlPO().showUndoLastRuleOperator()
        operator.undoLastRule()
    }
}