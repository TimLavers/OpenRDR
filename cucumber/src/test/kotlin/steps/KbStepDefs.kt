package steps

import io.cucumber.datatable.DataTable
import io.cucumber.docstring.DocString
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.kotest.matchers.shouldBe
import io.rippledown.integration.waitUntilAsserted

class KbStepDefs {

    @Then("the displayed KB name is (now ){word}")
    fun theDisplayedKBNameIsNow(kbName: String) {
        waitUntilAsserted {
            kbControlsPO().currentKB() shouldBe kbName
        }
    }

    @Then("I activate the KB management control")
    fun activateTheKBManagementControl() {
        kbControlsPO().expandDropdownMenu()
    }

    @Then("I (should )see this list of available KBs:")
    fun requireListOfAvailableKBs(dataTable: DataTable) {
        val expectedKBs = dataTable.asList()
        kbControlsPO().availableKBs() shouldBe expectedKBs
    }

    @Then("I create a Knowledge Base with the name {word}")
    fun createAKnowledgeBaseWithTheName(kbName: String) {
        kbControlsPO().createKB(kbName)
    }

    @Then("I create a Knowledge Base with the name {word} based on the {string} sample")
    fun createAKnowledgeBaseWithTheNameBasedOnSample(kbName: String, sampleTitle: String) {
        kbControlsPO().createKBFromSample(kbName, sampleTitle)
    }

    @Then("I select the Knowledge Base named {word}")
    fun selectTheKnowledgeBaseNamed(kbName: String) {
        kbControlsPO().selectKB(kbName)
    }

    @Then("the change interpretation icon (is )(should be )hidden")
    fun theChangeInterpretationIconShouldBeHidden() {
        interpretationViewPO().requireChangeInterpretationIconToBeHidden()
    }

    @Then("the change interpretation icon (is )(should be )shown")
    fun theChangeInterpretationIconShouldBeShown() {
        interpretationViewPO().requireChangeInterpretationIconToBeShowing()
    }

    @Then("the change interpretation dropdown menu (is )(should be )shown")
    fun theChangeInterpretationDropdownMenuShouldBeShown() {
        interpretationViewPO().requireChangeInterpretationDropDownMenuToBeShowing()
    }

    @Then("the change interpretation dropdown menu (is )(should be )hidden")
    fun theChangeInterpretationDropdownMenuShouldBeHidden() {
        interpretationViewPO().requireChangeInterpretationDropDownMenuToBeHidden()
    }

    @Then("the KB controls (are )(should be )hidden")
    fun theKBControlsAreShouldBeHidden() {
        kbControlsPO().requireKbControlsToBeHidden()
    }

    @Then("the KB controls (are )(should be )shown")
    fun theKBControlsAreShouldBeShown() {
        kbControlsPO().requireKbControlsToBeShown()
    }

    @When("I set the KB description to:")
    fun i_set_the_description_to(description: DocString) {
        val descriptionOperator = editCurrentKbControlPO().showDescriptionOperator()
        descriptionOperator.setDescription(description.content)
    }

    @Then("the KB description is:")
    fun the_KBDescriptionIsNow(description: DocString) {
        println("description: ${description.content}")
        val expectedText = description.content ?: ""
        println(expectedText)
        val descriptionOperator = editCurrentKbControlPO().showDescriptionOperator()
        descriptionOperator.description() shouldBe expectedText
        descriptionOperator.cancel()
    }
}