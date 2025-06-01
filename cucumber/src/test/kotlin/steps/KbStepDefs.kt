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
    @Then("the KB controls (are )(should be )hidden")
    fun theKBControlsAreShouldBeHidden() {
        kbControlsPO().requireKbControlsToBeHidden()
    }

    @Then("the KB controls (are )(should be )shown")
    fun theKBControlsAreShouldBeShown() {
        kbControlsPO().requireKbControlsToBeShown()
    }
}